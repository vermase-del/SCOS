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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final FacilityBookingRepository bookingRepository;

    public FacilityService(FacilityRepository facilityRepository,
                           FacilityBookingRepository bookingRepository) {
        this.facilityRepository = facilityRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public FacilityDto createFacility(UUID communityId, CreateFacilityRequest req, String createdBy) {
        if (facilityRepository.existsByCommunityIdAndFacilityCode(communityId, req.facilityCode().trim().toUpperCase())) {
            throw new IllegalArgumentException("Facility with code " + req.facilityCode() + " already exists in this community");
        }

        FacilityEntity entity = new FacilityEntity();
        entity.setCommunityId(communityId);
        entity.setFacilityCode(req.facilityCode().trim().toUpperCase());
        entity.setName(req.name().trim());
        entity.setDescription(req.description());
        entity.setCategory(req.category());
        entity.setBuildingId(req.buildingId());
        entity.setLocationDetails(req.locationDetails());
        if (req.capacity() != null) entity.setCapacity(req.capacity());
        if (req.hourlyFee() != null) entity.setHourlyFee(req.hourlyFee());
        if (req.securityDeposit() != null) entity.setSecurityDeposit(req.securityDeposit());
        if (req.currencyCode() != null) entity.setCurrencyCode(req.currencyCode());
        if (req.requiresApproval() != null) entity.setRequiresApproval(req.requiresApproval());
        if (req.operatingHoursStart() != null) entity.setOperatingHoursStart(req.operatingHoursStart());
        if (req.operatingHoursEnd() != null) entity.setOperatingHoursEnd(req.operatingHoursEnd());
        if (req.minBookingHours() != null) entity.setMinBookingHours(req.minBookingHours());
        if (req.maxBookingHours() != null) entity.setMaxBookingHours(req.maxBookingHours());
        if (req.cancellationCutoffHours() != null) entity.setCancellationCutoffHours(req.cancellationCutoffHours());
        entity.setRulesAndRegulations(req.rulesAndRegulations());
        entity.setIsActive(true);
        entity.setCreatedBy(createdBy != null ? createdBy : "system");
        entity.setUpdatedBy(createdBy != null ? createdBy : "system");

        FacilityEntity saved = facilityRepository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public FacilityDto updateFacility(UUID communityId, UUID facilityId, UpdateFacilityRequest req, String updatedBy) {
        FacilityEntity entity = facilityRepository.findByCommunityIdAndId(communityId, facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found: " + facilityId));

        entity.setName(req.name().trim());
        entity.setDescription(req.description());
        entity.setCategory(req.category());
        entity.setBuildingId(req.buildingId());
        entity.setLocationDetails(req.locationDetails());
        if (req.capacity() != null) entity.setCapacity(req.capacity());
        if (req.hourlyFee() != null) entity.setHourlyFee(req.hourlyFee());
        if (req.securityDeposit() != null) entity.setSecurityDeposit(req.securityDeposit());
        if (req.requiresApproval() != null) entity.setRequiresApproval(req.requiresApproval());
        if (req.operatingHoursStart() != null) entity.setOperatingHoursStart(req.operatingHoursStart());
        if (req.operatingHoursEnd() != null) entity.setOperatingHoursEnd(req.operatingHoursEnd());
        if (req.minBookingHours() != null) entity.setMinBookingHours(req.minBookingHours());
        if (req.maxBookingHours() != null) entity.setMaxBookingHours(req.maxBookingHours());
        if (req.cancellationCutoffHours() != null) entity.setCancellationCutoffHours(req.cancellationCutoffHours());
        entity.setRulesAndRegulations(req.rulesAndRegulations());
        if (req.isActive() != null) entity.setIsActive(req.isActive());
        entity.setUpdatedBy(updatedBy != null ? updatedBy : "system");

        FacilityEntity saved = facilityRepository.save(entity);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public FacilityDto getFacility(UUID communityId, UUID facilityId) {
        return facilityRepository.findByCommunityIdAndId(communityId, facilityId)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found: " + facilityId));
    }

    @Transactional(readOnly = true)
    public List<FacilityDto> listFacilities(UUID communityId, FacilityCategory category, Boolean activeOnly) {
        List<FacilityEntity> list;
        if (Boolean.TRUE.equals(activeOnly)) {
            if (category != null) {
                list = facilityRepository.findByCommunityIdAndCategoryAndIsActiveTrueOrderByNameAsc(communityId, category);
            } else {
                list = facilityRepository.findByCommunityIdAndIsActiveTrueOrderByNameAsc(communityId);
            }
        } else {
            list = facilityRepository.findByCommunityIdOrderByNameAsc(communityId);
        }
        return list.stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public FacilitySummaryDto getFacilitySummary(UUID communityId) {
        long totalFacilities = facilityRepository.countByCommunityId(communityId);
        long activeFacilities = facilityRepository.countByCommunityIdAndIsActiveTrue(communityId);
        long totalBookings = bookingRepository.countByCommunityIdAndStatus(communityId, BookingStatus.CONFIRMED);
        long bookingsToday = bookingRepository.countActiveBookingsOnDate(communityId, LocalDate.now());
        long pendingApprovals = bookingRepository.countByCommunityIdAndStatus(communityId, BookingStatus.PENDING_APPROVAL);

        return new FacilitySummaryDto(totalFacilities, activeFacilities, totalBookings, bookingsToday, pendingApprovals);
    }

    public FacilityDto mapToDto(FacilityEntity entity) {
        return new FacilityDto(
                entity.getId(),
                entity.getCommunityId(),
                entity.getFacilityCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getBuildingId(),
                entity.getLocationDetails(),
                entity.getCapacity(),
                entity.getHourlyFee(),
                entity.getSecurityDeposit(),
                entity.getCurrencyCode(),
                entity.getRequiresApproval(),
                entity.getOperatingHoursStart(),
                entity.getOperatingHoursEnd(),
                entity.getMinBookingHours(),
                entity.getMaxBookingHours(),
                entity.getCancellationCutoffHours(),
                entity.getRulesAndRegulations(),
                entity.getIsActive()
        );
    }
}
