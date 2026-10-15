package com.scos.modules.facility.repository;

import com.scos.modules.facility.entity.FacilityBookingHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FacilityBookingHistoryRepository extends JpaRepository<FacilityBookingHistoryEntity, UUID> {

    List<FacilityBookingHistoryEntity> findByCommunityIdAndBookingIdOrderByCreatedAtDesc(UUID communityId, UUID bookingId);
}
