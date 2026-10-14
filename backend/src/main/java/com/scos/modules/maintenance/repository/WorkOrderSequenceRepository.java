package com.scos.modules.maintenance.repository;
import com.scos.modules.maintenance.entity.WorkOrderSequenceEntity; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import org.springframework.stereotype.Repository; import org.springframework.transaction.annotation.Transactional; import java.util.UUID;
@Repository public interface WorkOrderSequenceRepository extends JpaRepository<WorkOrderSequenceEntity, UUID> {
 @Modifying @Transactional
 @Query(value="INSERT INTO scos_work_order_sequences (community_id, year, current_sequence) VALUES (:communityId, :year, 1) ON CONFLICT (community_id, year) DO UPDATE SET current_sequence = scos_work_order_sequences.current_sequence + 1", nativeQuery=true)
 void increment(@Param("communityId") UUID communityId, @Param("year") int year);
 @Query(value="SELECT current_sequence FROM scos_work_order_sequences WHERE community_id = :communityId AND year = :year", nativeQuery=true)
 long current(@Param("communityId") UUID communityId, @Param("year") int year);
}
