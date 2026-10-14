package com.scos.modules.maintenance.repository;
import com.scos.modules.maintenance.entity.WorkOrderAssignmentEntity; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface WorkOrderAssignmentRepository extends JpaRepository<WorkOrderAssignmentEntity, UUID> { List<WorkOrderAssignmentEntity> findByWorkOrderIdOrderByAssignedAtAsc(UUID workOrderId); }
