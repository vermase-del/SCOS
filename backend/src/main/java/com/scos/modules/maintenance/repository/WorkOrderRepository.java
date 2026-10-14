package com.scos.modules.maintenance.repository;

import com.scos.modules.maintenance.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.Instant;
import java.util.*;

public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, UUID>, JpaSpecificationExecutor<WorkOrderEntity> {
    Page<WorkOrderEntity> findByCommunityIdOrderByCreatedAtDesc(UUID communityId, Pageable pageable);
    Page<WorkOrderEntity> findByCommunityIdAndStatusOrderByCreatedAtDesc(UUID communityId, WorkOrderStatus status, Pageable pageable);
    Page<WorkOrderEntity> findByCommunityIdAndPriorityOrderByCreatedAtDesc(UUID communityId, WorkOrderPriority priority, Pageable pageable);
    Page<WorkOrderEntity> findByCommunityIdAndStatusAndPriorityOrderByCreatedAtDesc(UUID communityId, WorkOrderStatus status, WorkOrderPriority priority, Pageable pageable);
    Page<WorkOrderEntity> findByCommunityIdAndAssetIdOrderByCreatedAtDesc(UUID communityId, UUID assetId, Pageable pageable);
    Page<WorkOrderEntity> findByCommunityIdAndWorkOrderTypeOrderByCreatedAtDesc(UUID communityId, WorkOrderType workOrderType, Pageable pageable);
    Optional<WorkOrderEntity> findByIdAndCommunityId(UUID id, UUID communityId);
    long countByCommunityIdAndStatusIn(UUID communityId, Collection<WorkOrderStatus> statuses);
    long countByCommunityIdAndStatus(UUID communityId, WorkOrderStatus status);
    long countByCommunityIdAndSlaBreachedTrueAndStatusNotIn(UUID communityId, Collection<WorkOrderStatus> statuses);
    long countByCommunityIdAndSlaDueAtBeforeAndStatusNotIn(UUID communityId, Instant now, Collection<WorkOrderStatus> statuses);
}
