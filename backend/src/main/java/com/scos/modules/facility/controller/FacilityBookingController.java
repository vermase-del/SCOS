package com.scos.modules.facility.controller;

import com.scos.common.exception.ResourceNotFoundException;
import com.scos.common.model.ApiResponse;
import com.scos.modules.facility.dto.*;
import com.scos.modules.facility.entity.BookingStatus;
import com.scos.modules.facility.service.FacilityBookingService;
import com.scos.modules.user.entity.UserEntity;
import com.scos.modules.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/facility-bookings")
@Tag(name = "Facility Bookings & Reservations", description = "Amenity slot reservations, approval workflow, and schedule conflict resolution")
public class FacilityBookingController {

    private final FacilityBookingService bookingService;
    private final UserRepository userRepository;

    public FacilityBookingController(FacilityBookingService bookingService,
                                     UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCREEN:FACILITY_BOOKINGS') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasRole('OWNER') or hasRole('TENANT')")
    @Operation(summary = "Create an amenity reservation / slot booking")
    public ApiResponse<FacilityBookingDto> createBooking(
            @RequestParam UUID communityId,
            @Valid @RequestBody CreateBookingRequest request,
            Principal principal) {
        UserEntity user = resolveUser(principal);
        return ApiResponse.success("Booking submitted successfully",
                bookingService.createBooking(communityId, user.getId(), request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCREEN:FACILITY_BOOKINGS') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasRole('HOA_OFFICER')")
    @Operation(summary = "List community facility bookings with optional filtering")
    public ApiResponse<List<FacilityBookingDto>> listBookings(
            @RequestParam UUID communityId,
            @RequestParam(required = false) UUID residentUserId) {
        return ApiResponse.success(bookingService.listBookings(communityId, residentUserId));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('SCREEN:FACILITY_BOOKINGS') or hasRole('OWNER') or hasRole('TENANT') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER')")
    @Operation(summary = "List current user's facility bookings")
    public ApiResponse<List<FacilityBookingDto>> listMyBookings(
            @RequestParam UUID communityId,
            Principal principal) {
        UserEntity user = resolveUser(principal);
        return ApiResponse.success(bookingService.listBookings(communityId, user.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCREEN:FACILITY_BOOKINGS') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasRole('OWNER') or hasRole('TENANT')")
    @Operation(summary = "Get booking details by ID including audit history")
    public ApiResponse<FacilityBookingDto> getBooking(
            @PathVariable UUID id,
            @RequestParam UUID communityId) {
        return ApiResponse.success(bookingService.getBooking(communityId, id));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasRole('HOA_OFFICER')")
    @Operation(summary = "Approve or reject a pending facility booking")
    public ApiResponse<FacilityBookingDto> reviewBooking(
            @PathVariable UUID id,
            @RequestParam UUID communityId,
            @Valid @RequestBody ReviewBookingRequest request,
            Principal principal) {
        UserEntity admin = resolveUser(principal);
        return ApiResponse.success("Booking review processed",
                bookingService.reviewBooking(communityId, id, admin.getId(), request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('SCREEN:FACILITY_BOOKINGS') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasRole('OWNER') or hasRole('TENANT')")
    @Operation(summary = "Cancel a reservation before the cutoff window")
    public ApiResponse<FacilityBookingDto> cancelBooking(
            @PathVariable UUID id,
            @RequestParam UUID communityId,
            @Valid @RequestBody CancelBookingRequest request,
            Principal principal) {
        UserEntity user = resolveUser(principal);
        return ApiResponse.success("Booking cancelled successfully",
                bookingService.cancelBooking(communityId, id, user.getId(), request));
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAuthority('SCREEN:FACILITY_BOOKINGS') or hasRole('SUPER_ADMIN') or hasRole('COMMUNITY_MANAGER') or hasRole('OWNER') or hasRole('TENANT')")
    @Operation(summary = "Get booked slots for a facility on a specific date")
    public ApiResponse<List<BookingSlotDto>> getAvailability(
            @RequestParam UUID communityId,
            @RequestParam UUID facilityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(bookingService.getBookedSlotsForDate(communityId, facilityId, date));
    }

    private UserEntity resolveUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResourceNotFoundException("Authenticated principal not found");
        }
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + principal.getName()));
    }
}
