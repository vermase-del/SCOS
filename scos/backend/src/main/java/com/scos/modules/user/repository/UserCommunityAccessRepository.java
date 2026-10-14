package com.scos.modules.user.repository;

import com.scos.modules.user.entity.UserCommunityAccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserCommunityAccessRepository extends JpaRepository<UserCommunityAccessEntity, UUID> {
    List<UserCommunityAccessEntity> findByUserId(UUID userId);
    List<UserCommunityAccessEntity> findByCommunityId(UUID communityId);
    List<UserCommunityAccessEntity> findByUserIdAndCommunityId(UUID userId, UUID communityId);
}
