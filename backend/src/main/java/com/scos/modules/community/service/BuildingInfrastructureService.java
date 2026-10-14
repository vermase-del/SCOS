package com.scos.modules.community.service;

import com.scos.common.exception.ResourceNotFoundException;
import com.scos.modules.community.dto.BuildingDto;
import com.scos.modules.community.dto.BuildingFloorMatrixDto;
import com.scos.modules.community.dto.UnitDto;
import com.scos.modules.community.entity.BuildingEntity;
import com.scos.modules.community.entity.UnitEntity;
import com.scos.modules.community.repository.BuildingRepository;
import com.scos.modules.community.repository.UnitRepository;
import com.scos.security.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BuildingInfrastructureService {

    private final BuildingRepository buildingRepository;
    private final UnitRepository unitRepository;

    public BuildingInfrastructureService(BuildingRepository buildingRepository, UnitRepository unitRepository) {
        this.buildingRepository = buildingRepository;
        this.unitRepository = unitRepository;
    }

    @Transactional(readOnly = true)
    public List<BuildingDto> getBuildingsForCurrentCommunity() {
        UUID communityId = TenantContext.getCommunityId();
        if (communityId == null) {
            return buildingRepository.findAll().stream().map(this::mapToBuildingDto).collect(Collectors.toList());
        }
        return buildingRepository.findByCommunityId(communityId).stream()
                .map(this::mapToBuildingDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BuildingFloorMatrixDto getBuildingFloorMatrix(UUID buildingId) {
        BuildingEntity building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + buildingId));

        List<UnitEntity> units = unitRepository.findByBuildingIdOrderByFloorNumberDescUnitNumberAsc(buildingId);

        Map<Integer, List<BuildingFloorMatrixDto.UnitTileDto>> floorMap = new LinkedHashMap<>();
        for (UnitEntity unit : units) {
            floorMap.computeIfAbsent(unit.getFloorNumber(), k -> new ArrayList<>())
                    .add(new BuildingFloorMatrixDto.UnitTileDto(
                            unit.getId(),
                            unit.getUnitNumber(),
                            unit.getUnitType(),
                            unit.getOccupancyStatus(),
                            null
                    ));
        }

        List<BuildingFloorMatrixDto.FloorRowDto> floorRows = floorMap.entrySet().stream()
                .sorted(Map.Entry.<Integer, List<BuildingFloorMatrixDto.UnitTileDto>>comparingByKey().reversed())
                .map(e -> new BuildingFloorMatrixDto.FloorRowDto(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return new BuildingFloorMatrixDto(
                building.getId(),
                building.getCode(),
                building.getName(),
                building.getTotalFloors(),
                units.size(),
                floorRows
        );
    }

    @Transactional(readOnly = true)
    public Page<UnitDto> getUnits(UUID buildingId, String occupancyStatus, Pageable pageable) {
        UUID communityId = TenantContext.getCommunityId();
        Page<UnitEntity> page;

        if (buildingId != null && communityId != null) {
            page = unitRepository.findByCommunityIdAndBuildingId(communityId, buildingId, pageable);
        } else if (occupancyStatus != null && communityId != null) {
            page = unitRepository.findByCommunityIdAndOccupancyStatus(communityId, occupancyStatus, pageable);
        } else if (communityId != null) {
            page = unitRepository.findByCommunityId(communityId, pageable);
        } else {
            page = unitRepository.findAll(pageable);
        }

        return page.map(this::mapToUnitDto);
    }

    @Transactional
    public UnitDto updateUnitStatus(UUID unitId, String occupancyStatus) {
        UnitEntity unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + unitId));
        unit.setOccupancyStatus(occupancyStatus);
        unit = unitRepository.save(unit);
        return mapToUnitDto(unit);
    }

    private BuildingDto mapToBuildingDto(BuildingEntity b) {
        BuildingDto dto = new BuildingDto();
        dto.setId(b.getId());
        dto.setCommunityId(b.getCommunityId());
        dto.setCode(b.getCode());
        dto.setName(b.getName());
        dto.setTotalFloors(b.getTotalFloors());
        dto.setBasementFloors(b.getBasementFloors());
        dto.setTotalUnits(b.getTotalUnits());
        dto.setHasElevator(b.isHasElevator());
        dto.setConstructionStatus(b.getConstructionStatus());
        return dto;
    }

    private UnitDto mapToUnitDto(UnitEntity u) {
        UnitDto dto = new UnitDto();
        dto.setId(u.getId());
        dto.setCommunityId(u.getCommunityId());
        dto.setBuildingId(u.getBuildingId());
        dto.setUnitNumber(u.getUnitNumber());
        dto.setFloorNumber(u.getFloorNumber());
        dto.setUnitType(u.getUnitType());
        dto.setCarpetAreaSqft(u.getCarpetAreaSqft());
        dto.setSuperBuiltupAreaSqft(u.getSuperBuiltupAreaSqft());
        dto.setBalconyCount(u.getBalconyCount());
        dto.setBathroomCount(u.getBathroomCount());
        dto.setParkingSlotNumber(u.getParkingSlotNumber());
        dto.setOccupancyStatus(u.getOccupancyStatus());
        dto.setFurnishingStatus(u.getFurnishingStatus());
        dto.setElectricMeterNumber(u.getElectricMeterNumber());
        dto.setGasMeterNumber(u.getGasMeterNumber());
        dto.setWaterMeterNumber(u.getWaterMeterNumber());
        dto.setBaseMaintenanceCharge(u.getBaseMaintenanceCharge());
        dto.setActive(u.isActive());
        return dto;
    }
}
