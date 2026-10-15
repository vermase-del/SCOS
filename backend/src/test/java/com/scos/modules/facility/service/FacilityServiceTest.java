package com.scos.modules.facility.service;

import com.scos.common.exception.ResourceNotFoundException;
import com.scos.modules.facility.dto.CreateFacilityRequest;
import com.scos.modules.facility.dto.FacilityDto;
import com.scos.modules.facility.dto.FacilitySummaryDto;
import com.scos.modules.facility.dto.UpdateFacilityRequest;
import com.scos.modules.facility.entity.BookingStatus;
import com.scos.modules.facility.entity.FacilityCategory;
import com.scos.modules.facility.entity.FacilityEntity;
import com.scos.modules.facility.repository.FacilityBookingRepository;
import com.scos.modules.facility.repository.FacilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacilityServiceTest {

    @Mock private FacilityRepository facilityRepository;
    @Mock private FacilityBookingRepository bookingRepository;

    private FacilityService facilityService;
    private UUID communityId;

    @BeforeEach
    void setUp() {
        facilityService = new FacilityService(facilityRepository, bookingRepository);
        communityId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Create facility successfully with valid parameters")
    void createFacility_success() {
        CreateFacilityRequest req = new CreateFacilityRequest(
                "FAC-CH-01",
                "Grand Banquet Hall",
                "Spacious hall",
                FacilityCategory.PARTY_HALL,
                null,
                "Wing A",
                150,
                new BigDecimal("2000.00"),
                new BigDecimal("5000.00"),
                "INR",
                true,
                LocalTime.of(9, 0),
                LocalTime.of(23, 0),
                3,
                8,
                24,
                "No open fires"
        );

        when(facilityRepository.existsByCommunityIdAndFacilityCode(communityId, "FAC-CH-01")).thenReturn(false);
        when(facilityRepository.save(any(FacilityEntity.class))).thenAnswer(invocation -> {
            FacilityEntity e = invocation.getArgument(0);
            return e;
        });

        FacilityDto dto = facilityService.createFacility(communityId, req, "admin");

        assertNotNull(dto);
        assertEquals("FAC-CH-01", dto.facilityCode());
        assertEquals("Grand Banquet Hall", dto.name());
        assertEquals(FacilityCategory.PARTY_HALL, dto.category());
        assertEquals(150, dto.capacity());
        assertTrue(dto.requiresApproval());
        verify(facilityRepository).save(any(FacilityEntity.class));
    }

    @Test
    @DisplayName("Create facility with duplicate facilityCode in same community throws IllegalArgumentException")
    void createFacility_duplicateCode_throwsException() {
        CreateFacilityRequest req = new CreateFacilityRequest(
                "FAC-CH-01",
                "Duplicate Hall",
                null,
                FacilityCategory.PARTY_HALL,
                null,
                null,
                100,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "INR",
                false,
                LocalTime.of(8, 0),
                LocalTime.of(22, 0),
                1,
                4,
                2,
                null
        );

        when(facilityRepository.existsByCommunityIdAndFacilityCode(communityId, "FAC-CH-01")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> facilityService.createFacility(communityId, req, "admin"));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(facilityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update facility modifies fields and returns updated DTO")
    void updateFacility_success() {
        UUID facilityId = UUID.randomUUID();
        FacilityEntity existing = new FacilityEntity();
        existing.setCommunityId(communityId);
        existing.setFacilityCode("FAC-POOL-01");
        existing.setName("Olympic Pool");
        existing.setCategory(FacilityCategory.SWIMMING_POOL);
        existing.setCapacity(30);

        when(facilityRepository.findByCommunityIdAndId(communityId, facilityId))
                .thenReturn(Optional.of(existing));
        when(facilityRepository.save(any(FacilityEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateFacilityRequest updateReq = new UpdateFacilityRequest(
                "Olympic Pool Renovated",
                "New filtration system",
                FacilityCategory.SWIMMING_POOL,
                null,
                "Ground floor",
                50,
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                false,
                LocalTime.of(6, 0),
                LocalTime.of(22, 0),
                1,
                3,
                2,
                "Lycra swimwear required",
                true
        );

        FacilityDto updated = facilityService.updateFacility(communityId, facilityId, updateReq, "manager");

        assertEquals("Olympic Pool Renovated", updated.name());
        assertEquals(50, updated.capacity());
        assertEquals(new BigDecimal("200.00"), updated.hourlyFee());
        verify(facilityRepository).save(existing);
    }

    @Test
    @DisplayName("Get facility throws ResourceNotFoundException when ID does not exist")
    void getFacility_notFound_throwsException() {
        UUID randomId = UUID.randomUUID();
        when(facilityRepository.findByCommunityIdAndId(communityId, randomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> facilityService.getFacility(communityId, randomId));
    }

    @Test
    @DisplayName("Facility summary metrics computes correct counts")
    void getFacilitySummary_computesCounts() {
        when(facilityRepository.countByCommunityId(communityId)).thenReturn(5L);
        when(facilityRepository.countByCommunityIdAndIsActiveTrue(communityId)).thenReturn(4L);
        when(bookingRepository.countByCommunityIdAndStatus(communityId, BookingStatus.CONFIRMED)).thenReturn(12L);
        when(bookingRepository.countActiveBookingsOnDate(eq(communityId), any(LocalDate.class))).thenReturn(3L);
        when(bookingRepository.countByCommunityIdAndStatus(communityId, BookingStatus.PENDING_APPROVAL)).thenReturn(2L);

        FacilitySummaryDto summary = facilityService.getFacilitySummary(communityId);

        assertEquals(5L, summary.totalFacilities());
        assertEquals(4L, summary.activeFacilities());
        assertEquals(12L, summary.totalBookings());
        assertEquals(3L, summary.bookingsToday());
        assertEquals(2L, summary.pendingApprovals());
    }
}
