package com.scos.modules.community.service;

import com.scos.common.exception.ResourceNotFoundException;
import com.scos.modules.community.dto.CommunityDto;
import com.scos.modules.community.dto.CommunityRegistrationRequest;
import com.scos.modules.community.dto.CommunityRegistrationResponse;
import com.scos.modules.community.entity.BuildingEntity;
import com.scos.modules.community.entity.CommunityEntity;
import com.scos.modules.community.entity.UnitEntity;
import com.scos.modules.community.repository.BuildingRepository;
import com.scos.modules.community.repository.CommunityRepository;
import com.scos.modules.community.repository.UnitRepository;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserCommunityAccessRepository accessRepository;
    private final BuildingRepository buildingRepository;
    private final UnitRepository unitRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public CommunityService(
            CommunityRepository communityRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserCommunityAccessRepository accessRepository,
            BuildingRepository buildingRepository,
            UnitRepository unitRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accessRepository = accessRepository;
        this.buildingRepository = buildingRepository;
        this.unitRepository = unitRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional(readOnly = true)
    public List<CommunityDto> getAllCommunities() {
        return communityRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CommunityDto getCommunityById(UUID id) {
        CommunityEntity entity = communityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found with id: " + id));
        return mapToDto(entity);
    }

    @Transactional
    public CommunityRegistrationResponse registerCommunity(CommunityRegistrationRequest request) {
        // 1. Determine unique code
        String code = request.getCode();
        if (code == null || code.isBlank()) {
            code = request.getName().trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
            if (code.length() > 25) {
                code = code.substring(0, 25);
            }
        }
        if (communityRepository.existsByCode(code)) {
            code = code + "_" + (System.currentTimeMillis() % 1000);
        }

        // 2. Save Community
        CommunityEntity community = new CommunityEntity();
        community.setCode(code);
        community.setName(request.getName());
        community.setLegalName(request.getLegalName() != null && !request.getLegalName().isBlank()
                ? request.getLegalName()
                : request.getName() + " Residents Welfare Association");
        community.setRegistrationNumber(request.getRegistrationNumber());
        community.setTaxIdentifier(request.getTaxIdentifier());
        community.setAddressLine1(request.getAddressLine1());
        community.setAddressLine2(request.getAddressLine2());
        community.setCity(request.getCity());
        community.setStateProvince(request.getStateProvince());
        community.setPostalCode(request.getPostalCode());
        community.setCountryCode(request.getCountryCode() != null ? request.getCountryCode() : "IND");
        community.setPhone(request.getPhone());
        community.setEmail(request.getEmail());
        community.setCurrencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "INR");
        community.setTimezone(request.getTimezone() != null ? request.getTimezone() : "Asia/Kolkata");
        community.setStatus("ACTIVE");
        community = communityRepository.save(community);

        final CommunityEntity savedCommunity = community;

        // 3. Create or Link Community Manager Account
        UserEntity adminUser = userRepository.findByUsername(request.getAdminUsername())
                .or(() -> userRepository.findByEmail(request.getAdminEmail()))
                .orElseGet(() -> {
                    UserEntity newUser = new UserEntity();
                    newUser.setUsername(request.getAdminUsername());
                    newUser.setEmail(request.getAdminEmail());
                    newUser.setPhoneNumber(request.getAdminPhone());
                    newUser.setFirstName(request.getAdminFirstName());
                    newUser.setLastName(request.getAdminLastName());
                    newUser.setPasswordHash(passwordEncoder.encode(request.getAdminPassword()));
                    newUser.setActive(true);
                    newUser.setVerified(true);
                    return userRepository.save(newUser);
                });

        RoleEntity managerRole = roleRepository.findByCode("COMMUNITY_MANAGER")
                .orElseGet(() -> {
                    RoleEntity r = new RoleEntity(null, "COMMUNITY_MANAGER", "Community Manager", "Estate administrator", true);
                    return roleRepository.save(r);
                });

        UserCommunityAccessEntity access = new UserCommunityAccessEntity();
        access.setUser(adminUser);
        access.setCommunityId(savedCommunity.getId());
        access.setRole(managerRole);
        access.setStatus("ACTIVE");
        accessRepository.save(access);

        // 4. Optional Rapid Infrastructure Provisioning
        int towersCreated = 0;
        int unitsCreated = 0;
        if (request.getNumberOfTowers() > 0 && request.getFloorsPerTower() > 0 && request.getUnitsPerFloor() > 0) {
            for (int t = 0; t < request.getNumberOfTowers(); t++) {
                char towerChar = (char) ('A' + t);
                String bCode = "TOWER_" + towerChar;
                String bName = (request.getTowerPrefix() != null ? request.getTowerPrefix().trim() : "Tower") + " " + towerChar;

                BuildingEntity building = new BuildingEntity();
                building.setCommunityId(savedCommunity.getId());
                building.setCode(bCode);
                building.setName(bName);
                building.setTotalFloors(request.getFloorsPerTower());
                building.setBasementFloors(1);
                building.setTotalUnits(request.getFloorsPerTower() * request.getUnitsPerFloor());
                building.setHasElevator(true);
                building.setConstructionStatus("COMPLETED");
                building = buildingRepository.save(building);
                towersCreated++;

                List<UnitEntity> units = new ArrayList<>();
                for (int f = request.getFloorsPerTower(); f >= 1; f--) {
                    for (int u = 1; u <= request.getUnitsPerFloor(); u++) {
                        UnitEntity unit = new UnitEntity();
                        unit.setCommunityId(savedCommunity.getId());
                        unit.setBuildingId(building.getId());
                        unit.setUnitNumber(towerChar + "-" + (f * 100 + u));
                        unit.setFloorNumber(f);
                        boolean isEven = (u % 2 == 0);
                        unit.setUnitType(isEven ? "3BHK" : "2BHK");
                        unit.setCarpetAreaSqft(isEven ? new BigDecimal("1550.00") : new BigDecimal("1150.00"));
                        unit.setSuperBuiltupAreaSqft(isEven ? new BigDecimal("1920.00") : new BigDecimal("1420.00"));
                        unit.setBalconyCount(isEven ? 2 : 1);
                        unit.setBathroomCount(isEven ? 3 : 2);
                        unit.setOccupancyStatus("VACANT");
                        unit.setFurnishingStatus("SEMI_FURNISHED");
                        unit.setBaseMaintenanceCharge(isEven ? new BigDecimal("4800.00") : new BigDecimal("3500.00"));
                        units.add(unit);
                        unitsCreated++;
                    }
                }
                unitRepository.saveAll(units);
            }
        }

        // 5. Generate Initial JWT Token
        String token = tokenProvider.generateToken(
                adminUser.getId(),
                adminUser.getUsername(),
                List.of("COMMUNITY_MANAGER"),
                savedCommunity.getId()
        );

        return new CommunityRegistrationResponse(
                savedCommunity.getId(),
                savedCommunity.getCode(),
                savedCommunity.getName(),
                adminUser.getId(),
                adminUser.getUsername(),
                token,
                towersCreated,
                unitsCreated,
                "Community '" + savedCommunity.getName() + "' successfully registered and provisioned!"
        );
    }

    private CommunityDto mapToDto(CommunityEntity c) {
        CommunityDto dto = new CommunityDto();
        dto.setId(c.getId());
        dto.setCode(c.getCode());
        dto.setName(c.getName());
        dto.setLegalName(c.getLegalName());
        dto.setCity(c.getCity());
        dto.setStateProvince(c.getStateProvince());
        dto.setCountryCode(c.getCountryCode());
        dto.setPhone(c.getPhone());
        dto.setEmail(c.getEmail());
        dto.setCurrencyCode(c.getCurrencyCode());
        dto.setStatus(c.getStatus());
        return dto;
    }
}
