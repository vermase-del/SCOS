package com.scos.modules.maintenance.repository;
import com.scos.modules.maintenance.entity.WorkOrderCostLineEntity; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface WorkOrderCostLineRepository extends JpaRepository<WorkOrderCostLineEntity, UUID> { List<WorkOrderCostLineEntity> findByWorkOrderIdOrderByCreatedAtAsc(UUID workOrderId); }
