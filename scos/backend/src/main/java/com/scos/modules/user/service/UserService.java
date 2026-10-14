package com.scos.modules.user.service;

import com.scos.common.exception.ResourceNotFoundException;
import com.scos.modules.user.dto.*;
import com.scos.modules.user.entity.PermissionEntity;
import com.scos.modules.user.entity.RoleEntity;
import com.scos.modules.user.entity.UserCommunityAccessEntity;
import com.scos.modules.user.entity.UserEntity;
import com.scos.modules.user.repository.PermissionRepository;
import com.scos.modules.user.repository.RoleRepository;
import com.scos.modules.user.repository.UserCommunityAccessRepository;
import com.scos.modules.user.repository.UserRepository;
import com.scos.security.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserCommunityAccessRepository accessRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserCommunityAccessRepository accessRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.accessRepository = accessRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return mapToDto(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);
        user.setVerified(true);
        user = userRepository.save(user);

        UUID communityId = request.getCommunityId();
        if (communityId == null) {
            communityId = TenantContext.getCommunityId();
        }
        if (communityId == null) {
            // Default to any first community access or active context
            communityId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        }

        RoleEntity role = roleRepository.findByCode(request.getRoleCode())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with code: " + request.getRoleCode()));

        UserCommunityAccessEntity access = new UserCommunityAccessEntity();
        access.setUser(user);
        access.setCommunityId(communityId);
        access.setRole(role);
        access.setStatus("ACTIVE");
        accessRepository.save(access);

        return mapToDto(user);
    }

    @Transactional
    public void assignRoleToUser(UUID userId, UUID communityId, String roleCode) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        RoleEntity role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with code: " + roleCode));

        UserCommunityAccessEntity access = new UserCommunityAccessEntity();
        access.setUser(user);
        access.setCommunityId(communityId);
        access.setRole(role);
        access.setStatus("ACTIVE");

        accessRepository.save(access);
    }

    @Transactional
    public void toggleUserActiveStatus(UUID userId, boolean active) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setActive(active);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<RoleDto> getAllRolesWithPermissions() {
        return roleRepository.findAll().stream().map(role -> {
            List<String> perms = role.getPermissions().stream()
                    .map(PermissionEntity::getCode)
                    .sorted()
                    .collect(Collectors.toList());
            return new RoleDto(
                    role.getId(),
                    role.getCode(),
                    role.getName(),
                    role.getDescription(),
                    role.isSystemRole(),
                    perms
            );
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> new PermissionDto(p.getId(), p.getCode(), p.getModule(), p.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleDto updateRolePermissions(String roleCode, List<String> permissionCodes) {
        RoleEntity role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with code: " + roleCode));

        List<PermissionEntity> permissions = permissionRepository.findByCodeIn(permissionCodes);
        role.setPermissions(new HashSet<>(permissions));
        role = roleRepository.save(role);

        List<String> perms = role.getPermissions().stream()
                .map(PermissionEntity::getCode)
                .sorted()
                .collect(Collectors.toList());

        return new RoleDto(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.isSystemRole(),
                perms
        );
    }

    @Transactional(readOnly = true)
    public List<String> getUserPermissions(UUID userId) {
        List<UserCommunityAccessEntity> accessList = accessRepository.findByUserId(userId);
        Set<String> perms = new HashSet<>();
        for (UserCommunityAccessEntity access : accessList) {
            RoleEntity role = access.getRole();
            if (role != null && role.getPermissions() != null) {
                for (PermissionEntity p : role.getPermissions()) {
                    perms.add(p.getCode());
                }
            }
        }
        return new ArrayList<>(perms);
    }

    private UserDto mapToDto(UserEntity user) {
        List<UserCommunityAccessEntity> accessList = accessRepository.findByUserId(user.getId());
        List<String> roles = accessList.stream()
                .map(a -> a.getRole().getCode())
                .distinct()
                .collect(Collectors.toList());

        UUID communityId = TenantContext.getCommunityId();
        if (communityId == null && !accessList.isEmpty()) {
            communityId = accessList.get(0).getCommunityId();
        }

        Set<String> permissions = new HashSet<>();
        for (UserCommunityAccessEntity access : accessList) {
            RoleEntity role = access.getRole();
            if (role != null && role.getPermissions() != null) {
                for (PermissionEntity p : role.getPermissions()) {
                    permissions.add(p.getCode());
                }
            }
        }

        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setActive(user.isActive());
        dto.setVerified(user.isVerified());
        dto.setRoles(roles);
        dto.setPrimaryRole(roles.isEmpty() ? "RESIDENT" : roles.get(0));
        dto.setPermissions(new ArrayList<>(permissions));
        dto.setActiveCommunityId(communityId);
        return dto;
    }
}
