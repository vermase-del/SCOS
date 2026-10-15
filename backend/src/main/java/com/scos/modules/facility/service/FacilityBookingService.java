package com.scos.modules.facility.service;

import com.scos.common.exception.ResourceNotFoundException;
import com.scos.modules.facility.dto.*;
import com.scos.modules.facility.entity.BookingStatus;
import com.scos.modules.facility.entity.FacilityBookingEntity;
import com.scos.modules.facility.entity.FacilityBookingHistoryEntity;
import com.scos.modules.facility.entity.FacilityEntity;
import com.scos.modules.facility.repository.FacilityBookingHistoryRepository;
import com.scos.modules.facility.repository.FacilityBookingRepository;
import com.scos.modules.facility.repository.FacilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class FacilityBookingService {

    private final FacilityRepository facilityRepository;
    private final FacilityBookingRepository bookingRepository;
    private final FacilityBookingHistoryRepository historyRepository;

    public FacilityBookingService(FacilityRepository facilityRepository,
                                  FacilityBookingRepository bookingRepository,
                                  FacilityBookingHistoryRepository historyRepository) {
        this.facilityRepository = facilityRepository;
        this.bookingRepository = bookingRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public FacilityBookingDto createBooking(UUID communityId, UUID residentUserId, CreateBookingRequest req) {
        FacilityEntity facility = facilityRepository.findByCommunityIdAndId(communityId, req.facilityId())
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found: " + req.facilityId()));

        if (!Boolean.TRUE.equals(facility.getIsActive())) {
            throw new IllegalArgumentException("Facility " + facility.getName() + " is currently inactive and cannot be booked");
        }

        if (!req.endTime().isAfter(req.startTime())) {
            throw new IllegalArgumentException("Booking end time must be strictly after start time");
        }

        // Validate duration
        long durationMinutes = Duration.between(req.startTime(), req.endTime()).toMinutes();
        double durationHours = durationMinutes / 60.0;
        if (durationHours < facility.getMinBookingHours()) {
            throw new IllegalArgumentException("Booking duration (" + durationHours + " hrs) is below minimum of "
                    + facility.getMinBookingHours() + " hour(s)");
        }
        if (durationHours > facility.getMaxBookingHours()) {
            throw new IllegalArgumentException("Booking duration (" + durationHours + " hrs) exceeds maximum of "
                    + facility.getMaxBookingHours() + " hour(s)");
        }

        // Check for conflicting bookings (exclusive overlap)
        List<FacilityBookingEntity> conflicts = bookingRepository.findConflictingBookings(
                communityId, req.facilityId(), req.startTime(), req.endTime());
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("The selected time slot conflicts with an existing reservation ("
                    + conflicts.get(0).getBookingReference() + ")");
        }

        // Fee calculations
        BigDecimal totalFee = facility.getHourlyFee().multiply(BigDecimal.valueOf(durationHours))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal deposit = facility.getSecurityDeposit();

        // Determine status: pending approval if facility requires it, else confirmed
        BookingStatus initialStatus = Boolean.TRUE.equals(facility.getRequiresApproval())
                ? BookingStatus.PENDING_APPROVAL
                : BookingStatus.CONFIRMED;

        // Generate Reference
        long bookingCount = bookingRepository.count();
        String reference = "FB-" + Year.now().getValue() + "-" + String.format("%06d", bookingCount + 1);

        FacilityBookingEntity booking = new FacilityBookingEntity(
                communityId,
                reference,
                facility,
                residentUserId,
                req.bookingDate(),
                req.startTime(),
                req.endTime()
        );
        booking.setUnitId(req.unitId());
        booking.setAttendeesCount(req.attendeesCount() != null ? req.attendeesCount() : 1);
        booking.setPurpose(req.purpose());
        booking.setStatus(initialStatus);
        booking.setTotalFee(totalFee);
        booking.setDepositAmount(deposit);
        booking.setCurrencyCode(facility.getCurrencyCode());

        FacilityBookingEntity saved = bookingRepository.save(booking);

        // Record History
        FacilityBookingHistoryEntity history = new FacilityBookingHistoryEntity(
                communityId,
                saved.getId(),
                null,
                initialStatus.name(),
                initialStatus == BookingStatus.PENDING_APPROVAL ? "Booking submitted for review" : "Booking instantly confirmed",
                residentUserId
        );
        historyRepository.save(history);

        return mapToDto(saved);
    }

    @Transactional
    public FacilityBookingDto reviewBooking(UUID communityId, UUID bookingId, UUID adminUserId, ReviewBookingRequest req) {
        FacilityBookingEntity booking = bookingRepository.findByCommunityIdAndId(communityId, bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only bookings in PENDING_APPROVAL status can be reviewed. Current status: " + booking.getStatus());
        }

        String oldStatus = booking.getStatus().name();
        if (Boolean.TRUE.equals(req.approved())) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setApprovedByUserId(adminUserId);
            booking.setApprovedAt(Instant.now());
        } else {
            booking.setStatus(BookingStatus.REJECTED);
            booking.setApprovedByUserId(adminUserId);
            booking.setRejectionReason(req.rejectionReason());
        }

        FacilityBookingEntity saved = bookingRepository.save(booking);

        FacilityBookingHistoryEntity history = new FacilityBookingHistoryEntity(
                communityId,
                saved.getId(),
                oldStatus,
                saved.getStatus().name(),
                Boolean.TRUE.equals(req.approved()) ? "Approved by administrator" : "Rejected: " + req.rejectionReason(),
                adminUserId
        );
        historyRepository.save(history);

        return mapToDto(saved);
    }

    @Transactional
    public FacilityBookingDto cancelBooking(UUID communityId, UUID bookingId, UUID actorUserId, CancelBookingRequest req) {
        FacilityBookingEntity booking = bookingRepository.findByCommunityIdAndId(communityId, bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED) {
            throw new IllegalStateException("Booking is already " + booking.getStatus());
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Completed bookings cannot be cancelled");
        }

        // Check cancellation cutoff
        int cutoffHours = booking.getFacility().getCancellationCutoffHours();
        if (cutoffHours > 0) {
            Instant cutoffTime = booking.getStartTime().minus(cutoffHours, ChronoUnit.HOURS);
            if (Instant.now().isAfter(cutoffTime)) {
                throw new IllegalStateException("Cancellation is not allowed within " + cutoffHours
                        + " hour(s) of reservation start time");
            }
        }

        String oldStatus = booking.getStatus().name();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledByUserId(actorUserId);
        booking.setCancelledAt(Instant.now());
        booking.setCancellationReason(req.reason());

        FacilityBookingEntity saved = bookingRepository.save(booking);

        FacilityBookingHistoryEntity history = new FacilityBookingHistoryEntity(
                communityId,
                saved.getId(),
                oldStatus,
                BookingStatus.CANCELLED.name(),
                "Cancelled: " + req.reason(),
                actorUserId
        );
        historyRepository.save(history);

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public FacilityBookingDto getBooking(UUID communityId, UUID bookingId) {
        return bookingRepository.findByCommunityIdAndId(communityId, bookingId)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    }

    @Transactional(readOnly = true)
    public List<FacilityBookingDto> listBookings(UUID communityId, UUID residentUserId) {
        List<FacilityBookingEntity> bookings;
        if (residentUserId != null) {
            bookings = bookingRepository.findByCommunityIdAndResidentUserIdOrderByStartTimeDesc(communityId, residentUserId);
        } else {
            bookings = bookingRepository.findByCommunityIdOrderByStartTimeDesc(communityId);
        }
        return bookings.stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<FacilityBookingDto> listFacilityBookingsOnDate(UUID communityId, UUID facilityId, LocalDate date) {
        return bookingRepository.findByCommunityIdAndFacilityIdAndBookingDateOrderByStartTimeAsc(communityId, facilityId, date)
                .stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingSlotDto> getBookedSlotsForDate(UUID communityId, UUID facilityId, LocalDate date) {
        return bookingRepository.findByCommunityIdAndFacilityIdAndBookingDateOrderByStartTimeAsc(communityId, facilityId, date)
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING_APPROVAL)
                .map(b -> new BookingSlotDto(b.getStartTime(), b.getEndTime(), false, b.getBookingReference()))
                .toList();
    }

    public FacilityBookingDto mapToDto(FacilityBookingEntity b) {
        return new FacilityBookingDto(
                b.getId(),
                b.getCommunityId(),
                b.getBookingReference(),
                b.getFacility().getId(),
                b.getFacility().getFacilityCode(),
                b.getFacility().getName(),
                b.getFacility().getCategory(),
                b.getResidentUserId(),
                b.getUnitId(),
                b.getBookingDate(),
                b.getStartTime(),
                b.getEndTime(),
                b.getAttendeesCount(),
                b.getPurpose(),
                b.getStatus(),
                b.getTotalFee(),
                b.getDepositAmount(),
                b.getCurrencyCode(),
                b.getApprovedByUserId(),
                b.getApprovedAt(),
                b.getRejectionReason(),
                b.getCancelledByUserId(),
                b.getCancelledAt(),
                b.getCancellationReason(),
                b.getCreatedAt()
        );
    }
}
