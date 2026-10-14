package com.scos.modules.community.repository;

import com.scos.modules.community.entity.CommunityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityRepository extends JpaRepository<CommunityEntity, UUID> {
    Optional<CommunityEntity> findByCode(String code);
    boolean existsByCode(String code);
}
