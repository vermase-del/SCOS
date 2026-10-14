package com.scos.modules.maintenance.repository;
import com.scos.modules.maintenance.entity.WorkOrderSlaHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface WorkOrderSlaHistoryRepository extends JpaRepository<WorkOrderSlaHistoryEntity,UUID> {
 List<WorkOrderSlaHistoryEntity> findByWorkOrderIdOrderByCreatedAtAsc(UUID workOrderId);
}
