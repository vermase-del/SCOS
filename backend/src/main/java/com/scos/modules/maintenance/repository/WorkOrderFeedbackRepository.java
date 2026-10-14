package com.scos.modules.maintenance.repository;
import com.scos.modules.maintenance.entity.WorkOrderFeedbackEntity; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface WorkOrderFeedbackRepository extends JpaRepository<WorkOrderFeedbackEntity, UUID> { Optional<WorkOrderFeedbackEntity> findByWorkOrderId(UUID workOrderId); List<WorkOrderFeedbackEntity> findByWorkOrderIdIn(Collection<UUID> workOrderIds); }
