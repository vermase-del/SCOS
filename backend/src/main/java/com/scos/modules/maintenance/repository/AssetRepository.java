package com.scos.modules.maintenance.repository;

import com.scos.modules.maintenance.entity.AssetEntity;
import com.scos.modules.maintenance.entity.AssetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<AssetEntity, UUID> {
    Page<AssetEntity> findByCommunityIdAndActiveTrueOrderByNameAsc(UUID communityId, Pageable pageable);
    Page<AssetEntity> findByCommunityIdAndActiveTrueAndStatusOrderByNameAsc(UUID communityId, AssetStatus status, Pageable pageable);
    Optional<AssetEntity> findByIdAndCommunityId(UUID id, UUID communityId);
    boolean existsByCommunityIdAndAssetCodeIgnoreCase(UUID communityId, String assetCode);
}
