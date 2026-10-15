package com.scos.modules.facility.repository;

import com.scos.modules.facility.entity.BookingStatus;
import com.scos.modules.facility.entity.FacilityBookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacilityBookingRepository extends JpaRepository<FacilityBookingEntity, UUID> {

    List<FacilityBookingEntity> findByCommunityIdOrderByStartTimeDesc(UUID communityId);

    List<FacilityBookingEntity> findByCommunityIdAndResidentUserIdOrderByStartTimeDesc(UUID communityId, UUID residentUserId);

    List<FacilityBookingEntity> findByCommunityIdAndFacilityIdAndBookingDateOrderByStartTimeAsc(
            UUID communityId, UUID facilityId, LocalDate bookingDate);

    Optional<FacilityBookingEntity> findByCommunityIdAndId(UUID communityId, UUID id);

    Optional<FacilityBookingEntity> findByCommunityIdAndBookingReference(UUID communityId, String bookingReference);

    @Query("SELECT b FROM FacilityBookingEntity b WHERE b.communityId = :communityId " +
            "AND b.facility.id = :facilityId " +
            "AND b.status IN (com.scos.modules.facility.entity.BookingStatus.PENDING_APPROVAL, com.scos.modules.facility.entity.BookingStatus.CONFIRMED) " +
            "AND (:startTime < b.endTime AND :endTime > b.startTime)")
    List<FacilityBookingEntity> findConflictingBookings(@Param("communityId") UUID communityId,
                                                        @Param("facilityId") UUID facilityId,
                                                        @Param("startTime") Instant startTime,
                                                        @Param("endTime") Instant endTime);

    long countByCommunityIdAndStatus(UUID communityId, BookingStatus status);

    @Query("SELECT COUNT(b) FROM FacilityBookingEntity b WHERE b.communityId = :communityId " +
            "AND b.bookingDate = :date AND b.status IN (com.scos.modules.facility.entity.BookingStatus.CONFIRMED, com.scos.modules.facility.entity.BookingStatus.CHECKED_IN)")
    long countActiveBookingsOnDate(@Param("communityId") UUID communityId, @Param("date") LocalDate date);
}
