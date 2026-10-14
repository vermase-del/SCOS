package com.scos.modules.maintenance.repository;
import com.scos.modules.maintenance.entity.WorkOrderActivityEntity; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface WorkOrderActivityRepository extends JpaRepository<WorkOrderActivityEntity, UUID> { List<WorkOrderActivityEntity> findByWorkOrderIdOrderByCreatedAtAsc(UUID workOrderId); }
