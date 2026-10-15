package com.scos.modules.facility.service;

import com.scos.common.exception.ResourceNotFoundException;
import com.scos.modules.facility.dto.CancelBookingRequest;
import com.scos.modules.facility.dto.CreateBookingRequest;
import com.scos.modules.facility.dto.FacilityBookingDto;
import com.scos.modules.facility.dto.ReviewBookingRequest;
import com.scos.modules.facility.entity.BookingStatus;
import com.scos.modules.facility.entity.FacilityBookingEntity;
import com.scos.modules.facility.entity.FacilityBookingHistoryEntity;
import com.scos.modules.facility.entity.FacilityCategory;
import com.scos.modules.facility.entity.FacilityEntity;
import com.scos.modules.facility.repository.FacilityBookingHistoryRepository;
import com.scos.modules.facility.repository.FacilityBookingRepository;
import com.scos.modules.facility.repository.FacilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacilityBookingServiceTest {

    @Mock private FacilityRepository facilityRepository;
    @Mock private FacilityBookingRepository bookingRepository;
    @Mock private FacilityBookingHistoryRepository historyRepository;

    private FacilityBookingService bookingService;
    private UUID communityId;
    private UUID residentUserId;
    private UUID facilityId;
    private FacilityEntity sampleFacility;

    @BeforeEach
    void setUp() {
        bookingService = new FacilityBookingService(facilityRepository, bookingRepository, historyRepository);
        communityId = UUID.randomUUID();
        residentUserId = UUID.randomUUID();
        facilityId = UUID.randomUUID();

        sampleFacility = new FacilityEntity();
        sampleFacility.setId(facilityId);
        sampleFacility.setCommunityId(communityId);
        sampleFacility.setFacilityCode("FAC-BADM-01");
        sampleFacility.setName("Indoor Badminton Court A");
        sampleFacility.setCategory(FacilityCategory.SPORTS_COURT);
        sampleFacility.setCapacity(6);
        sampleFacility.setHourlyFee(new BigDecimal("300.00"));
        sampleFacility.setSecurityDeposit(BigDecimal.ZERO);
        sampleFacility.setCurrencyCode("INR");
        sampleFacility.setOperatingHoursStart(LocalTime.of(6, 0));
        sampleFacility.setOperatingHoursEnd(LocalTime.of(22, 0));
        sampleFacility.setMinBookingHours(1);
        sampleFacility.setMaxBookingHours(3);
        sampleFacility.setCancellationCutoffHours(4);
        sampleFacility.setRequiresApproval(false);
        sampleFacility.setIsActive(true);
    }

    @Test
    @DisplayName("Create booking with direct confirmation when approval is not required")
    void createBooking_success_directConfirmed() {
        LocalDate date = LocalDate.now().plusDays(2);
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(
                facilityId, null, date, start, end, 4, "Casual friendly match"
        );

        when(facilityRepository.findByCommunityIdAndId(communityId, facilityId))
                .thenReturn(Optional.of(sampleFacility));
        when(bookingRepository.findConflictingBookings(eq(communityId), eq(facilityId), any(), any()))
                .thenReturn(List.of());
        when(bookingRepository.count()).thenReturn(5L);
        when(bookingRepository.save(any(FacilityBookingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FacilityBookingDto dto = bookingService.createBooking(communityId, residentUserId, req);

        assertNotNull(dto);
        assertEquals(BookingStatus.CONFIRMED, dto.status());
        assertEquals(new BigDecimal("600.00"), dto.totalFee());
        verify(historyRepository).save(any(FacilityBookingHistoryEntity.class));
    }

    @Test
    @DisplayName("Create booking routes to PENDING_APPROVAL when facility requires approval")
    void createBooking_requiresApproval() {
        sampleFacility.setRequiresApproval(true);
        sampleFacility.setHourlyFee(new BigDecimal("1000.00"));

        LocalDate date = LocalDate.now().plusDays(3);
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(
                facilityId, null, date, start, end, 50, "Society Club Meeting"
        );

        when(facilityRepository.findByCommunityIdAndId(communityId, facilityId))
                .thenReturn(Optional.of(sampleFacility));
        when(bookingRepository.findConflictingBookings(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(bookingRepository.count()).thenReturn(10L);
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FacilityBookingDto dto = bookingService.createBooking(communityId, residentUserId, req);

        assertEquals(BookingStatus.PENDING_APPROVAL, dto.status());
    }

    @Test
    @DisplayName("Create booking rejects conflicting overlapping reservation slot")
    void createBooking_conflictingSlot_throwsException() {
        LocalDate date = LocalDate.now().plusDays(1);
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        CreateBookingRequest req = new CreateBookingRequest(
                facilityId, null, date, start, end, 2, "Practice"
        );

        FacilityBookingEntity conflict = new FacilityBookingEntity();
        conflict.setBookingReference("BK-2026-0042");

        when(facilityRepository.findByCommunityIdAndId(communityId, facilityId))
                .thenReturn(Optional.of(sampleFacility));
        when(bookingRepository.findConflictingBookings(eq(communityId), eq(facilityId), any(), any()))
                .thenReturn(List.of(conflict));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.createBooking(communityId, residentUserId, req));

        assertTrue(ex.getMessage().contains("conflicts with an existing reservation"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create booking rejects duration below minimum booking hours")
    void createBooking_durationTooShort_throwsException() {
        LocalDate date = LocalDate.now().plusDays(1);
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant end = start.plus(30, ChronoUnit.MINUTES); // 0.5h, below min of 1h

        CreateBookingRequest req = new CreateBookingRequest(
                facilityId, null, date, start, end, 2, "Quick drill"
        );

        when(facilityRepository.findByCommunityIdAndId(communityId, facilityId))
                .thenReturn(Optional.of(sampleFacility));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(communityId, residentUserId, req));

        assertTrue(ex.getMessage().contains("below minimum"));
    }

    @Test
    @DisplayName("Review booking approval updates status to CONFIRMED and logs audit")
    void reviewBooking_approved() {
        UUID bookingId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        FacilityBookingEntity booking = new FacilityBookingEntity();
        booking.setId(bookingId);
        booking.setCommunityId(communityId);
        booking.setFacility(sampleFacility);
        booking.setStatus(BookingStatus.PENDING_APPROVAL);

        when(bookingRepository.findByCommunityIdAndId(communityId, bookingId))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReviewBookingRequest req = new ReviewBookingRequest(true, null);
        FacilityBookingDto result = bookingService.reviewBooking(communityId, bookingId, adminId, req);

        assertEquals(BookingStatus.CONFIRMED, result.status());
        assertEquals(adminId, result.approvedByUserId());
        verify(historyRepository).save(any(FacilityBookingHistoryEntity.class));
    }

    @Test
    @DisplayName("Review booking rejection updates status to REJECTED with reason")
    void reviewBooking_rejected() {
        UUID bookingId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        FacilityBookingEntity booking = new FacilityBookingEntity();
        booking.setId(bookingId);
        booking.setCommunityId(communityId);
        booking.setFacility(sampleFacility);
        booking.setStatus(BookingStatus.PENDING_APPROVAL);

        when(bookingRepository.findByCommunityIdAndId(communityId, bookingId))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReviewBookingRequest req = new ReviewBookingRequest(false, "Maintenance scheduled during requested slot");
        FacilityBookingDto result = bookingService.reviewBooking(communityId, bookingId, adminId, req);

        assertEquals(BookingStatus.REJECTED, result.status());
        assertEquals("Maintenance scheduled during requested slot", result.rejectionReason());
    }

    @Test
    @DisplayName("Cancel booking succeeds when requested before cancellation cutoff window")
    void cancelBooking_beforeCutoff_success() {
        UUID bookingId = UUID.randomUUID();

        // Start time 24 hours in future (cutoff is 4 hours)
        Instant start = Instant.now().plus(24, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        FacilityBookingEntity booking = new FacilityBookingEntity();
        booking.setId(bookingId);
        booking.setCommunityId(communityId);
        booking.setFacility(sampleFacility);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findByCommunityIdAndId(communityId, bookingId))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CancelBookingRequest req = new CancelBookingRequest("Change of travel plans");
        FacilityBookingDto result = bookingService.cancelBooking(communityId, bookingId, residentUserId, req);

        assertEquals(BookingStatus.CANCELLED, result.status());
        assertEquals("Change of travel plans", result.cancellationReason());
        verify(historyRepository).save(any(FacilityBookingHistoryEntity.class));
    }

    @Test
    @DisplayName("Cancel booking throws exception when requested within cancellation cutoff window")
    void cancelBooking_withinCutoff_throwsException() {
        UUID bookingId = UUID.randomUUID();

        // Start time 1 hour in future (cutoff is 4 hours)
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        FacilityBookingEntity booking = new FacilityBookingEntity();
        booking.setId(bookingId);
        booking.setCommunityId(communityId);
        booking.setFacility(sampleFacility);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findByCommunityIdAndId(communityId, bookingId))
                .thenReturn(Optional.of(booking));

        CancelBookingRequest req = new CancelBookingRequest("Late cancellation");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.cancelBooking(communityId, bookingId, residentUserId, req));

        assertTrue(ex.getMessage().contains("Cancellation is not allowed within"));
        verify(bookingRepository, never()).save(any());
    }
}
