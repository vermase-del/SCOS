package com.scos.modules.maintenance.repository;
import com.scos.modules.maintenance.entity.*; import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, UUID> {
 Page<WorkOrderEntity> findByCommunityIdOrderByCreatedAtDesc(UUID communityId, Pageable pageable);
 Page<WorkOrderEntity> findByCommunityIdAndStatusOrderByCreatedAtDesc(UUID communityId, WorkOrderStatus status, Pageable pageable);
 Page<WorkOrderEntity> findByCommunityIdAndPriorityOrderByCreatedAtDesc(UUID communityId, WorkOrderPriority priority, Pageable pageable);
 Optional<WorkOrderEntity> findByIdAndCommunityId(UUID id, UUID communityId);
 Page<WorkOrderEntity> findByCommunityIdAndStatusAndPriorityOrderByCreatedAtDesc(UUID communityId, WorkOrderStatus status, WorkOrderPriority priority, Pageable pageable);
 long countByCommunityIdAndStatusIn(UUID communityId, Collection<WorkOrderStatus> statuses);
 long countByCommunityIdAndStatus(UUID communityId, WorkOrderStatus status);
 long countByCommunityIdAndSlaBreachedTrueAndStatusNotIn(UUID communityId, Collection<WorkOrderStatus> statuses);
 long countByCommunityIdAndSlaDueAtBeforeAndStatusNotIn(UUID communityId, java.time.Instant now, Collection<WorkOrderStatus> statuses);
}
