package com.scos.modules.community.repository;

import com.scos.modules.community.entity.BuildingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BuildingRepository extends JpaRepository<BuildingEntity, UUID> {
    List<BuildingEntity> findByCommunityId(UUID communityId);
    Optional<BuildingEntity> findByCommunityIdAndCode(UUID communityId, String code);
}
