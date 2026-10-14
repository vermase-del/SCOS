package com.scos.modules.maintenance.repository;

import com.scos.modules.maintenance.entity.AssetStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AssetStatusHistoryRepository extends JpaRepository<AssetStatusHistoryEntity, UUID> {
    List<AssetStatusHistoryEntity> findByCommunityIdAndAssetIdOrderByChangedAtDesc(UUID communityId, UUID assetId);
}
