package com.scos.modules.user.repository;

import com.scos.modules.user.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {
    Optional<PermissionEntity> findByCode(String code);
    List<PermissionEntity> findByModule(String module);
    List<PermissionEntity> findByCodeIn(List<String> codes);
}
