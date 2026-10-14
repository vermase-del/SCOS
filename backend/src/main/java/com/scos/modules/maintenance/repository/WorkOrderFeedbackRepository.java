package com.scos.modules.maintenance.repository;
import com.scos.modules.maintenance.entity.WorkOrderFeedbackEntity; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param; import java.util.*;
public interface WorkOrderFeedbackRepository extends JpaRepository<WorkOrderFeedbackEntity, UUID> { Optional<WorkOrderFeedbackEntity> findByWorkOrderId(UUID workOrderId);
 @Query("select avg(f.rating) from WorkOrderFeedbackEntity f, WorkOrderEntity w where f.workOrderId = w.id and w.communityId = :communityId") Optional<Double> averageRatingForCommunity(@Param("communityId") UUID communityId); List<WorkOrderFeedbackEntity> findByWorkOrderIdIn(Collection<UUID> workOrderIds); }
