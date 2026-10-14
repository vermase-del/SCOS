package com.scos.modules.community.repository;

import com.scos.modules.community.entity.UnitEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnitRepository extends JpaRepository<UnitEntity, UUID> {
    List<UnitEntity> findByBuildingIdOrderByFloorNumberDescUnitNumberAsc(UUID buildingId);
    Page<UnitEntity> findByCommunityId(UUID communityId, Pageable pageable);
    Page<UnitEntity> findByCommunityIdAndBuildingId(UUID communityId, UUID buildingId, Pageable pageable);
    Page<UnitEntity> findByCommunityIdAndOccupancyStatus(UUID communityId, String occupancyStatus, Pageable pageable);
    Optional<UnitEntity> findByBuildingIdAndUnitNumber(UUID buildingId, String unitNumber);
    long countByCommunityIdAndOccupancyStatus(UUID communityId, String occupancyStatus);
}
