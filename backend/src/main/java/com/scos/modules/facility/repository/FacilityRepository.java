package com.scos.modules.facility.repository;

import com.scos.modules.facility.entity.FacilityCategory;
import com.scos.modules.facility.entity.FacilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacilityRepository extends JpaRepository<FacilityEntity, UUID> {

    List<FacilityEntity> findByCommunityIdAndIsActiveTrueOrderByNameAsc(UUID communityId);

    List<FacilityEntity> findByCommunityIdOrderByNameAsc(UUID communityId);

    List<FacilityEntity> findByCommunityIdAndCategoryAndIsActiveTrueOrderByNameAsc(UUID communityId, FacilityCategory category);

    Optional<FacilityEntity> findByCommunityIdAndId(UUID communityId, UUID id);

    Optional<FacilityEntity> findByCommunityIdAndFacilityCode(UUID communityId, String facilityCode);

    boolean existsByCommunityIdAndFacilityCode(UUID communityId, String facilityCode);

    long countByCommunityIdAndIsActiveTrue(UUID communityId);

    long countByCommunityId(UUID communityId);
}
