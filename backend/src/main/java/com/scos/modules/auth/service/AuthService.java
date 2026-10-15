package com.scos.modules.auth.service;

import com.scos.common.exception.ResourceNotFoundException;
import com.scos.common.exception.UnauthorizedException;
import com.scos.modules.auth.dto.AuthResponse;
import com.scos.modules.auth.dto.LoginRequest;
import com.scos.modules.auth.dto.RegisterRequest;
import com.scos.modules.user.dto.UserDto;
import com.scos.modules.user.entity.RoleEntity;
import com.scos.modules.user.entity.UserCommunityAccessEntity;
import com.scos.modules.user.entity.UserEntity;
import com.scos.modules.user.repository.RoleRepository;
import com.scos.modules.user.repository.UserCommunityAccessRepository;
import com.scos.modules.user.repository.UserRepository;
import com.scos.security.jwt.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scos.modules.user.entity.PermissionEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserCommunityAccessRepository accessRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, UserCommunityAccessRepository accessRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accessRepository = accessRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        try {
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
        } catch (Exception ex) {
            log.warn("Non-fatal: could not update lastLoginAt for user {}: {}", user.getUsername(), ex.getMessage());
        }

        List<UserCommunityAccessEntity> accessList = accessRepository.findByUserId(user.getId());
        List<String> roles = accessList.stream()
                .map(a -> a.getRole().getCode())
                .distinct()
                .collect(Collectors.toList());

        UUID activeCommunityId = request.getCommunityId();
        if (activeCommunityId == null && !accessList.isEmpty()) {
            activeCommunityId = accessList.get(0).getCommunityId();
        }

        Set<String> permissionsSet = new HashSet<>();
        for (UserCommunityAccessEntity access : accessList) {
            RoleEntity role = access.getRole();
            if (role != null && role.getPermissions() != null) {
                for (PermissionEntity p : role.getPermissions()) {
                    permissionsSet.add(p.getCode());
                }
            }
        }
        List<String> permissions = new ArrayList<>(permissionsSet);

        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), roles, permissions, activeCommunityId);

        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setPhoneNumber(user.getPhoneNumber());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setAvatarUrl(user.getAvatarUrl());
        userDto.setActive(user.isActive());
        userDto.setVerified(user.isVerified());
        userDto.setRoles(roles);
        userDto.setPrimaryRole(roles.isEmpty() ? "RESIDENT" : roles.get(0));
        userDto.setPermissions(permissions);
        userDto.setActiveCommunityId(activeCommunityId);

        return new AuthResponse(token, "Bearer", userDto);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already in use");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already in use");
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setActive(true);
        user.setVerified(true);

        user = userRepository.save(user);

        String roleCode = request.getRoleCode() != null ? request.getRoleCode() : "TENANT";
        RoleEntity role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with code: " + roleCode));

        if (request.getCommunityId() != null) {
            UserCommunityAccessEntity access = new UserCommunityAccessEntity();
            access.setUser(user);
            access.setCommunityId(request.getCommunityId());
            access.setRole(role);
            access.setStatus("ACTIVE");
            accessRepository.save(access);
        }

        List<String> roles = List.of(role.getCode());
        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), roles, request.getCommunityId());

        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setPhoneNumber(user.getPhoneNumber());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setActive(user.isActive());
        userDto.setVerified(user.isVerified());
        userDto.setRoles(roles);
        userDto.setActiveCommunityId(request.getCommunityId());

        return new AuthResponse(token, "Bearer", userDto);
    }
}
