package com.scos.config;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.scos.modules.user.entity.PermissionEntity;
import com.scos.modules.user.repository.PermissionRepository;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserCommunityAccessRepository accessRepository;
    private final CommunityRepository communityRepository;
    private final BuildingRepository buildingRepository;
    private final UnitRepository unitRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository, PermissionRepository permissionRepository, UserCommunityAccessRepository accessRepository, CommunityRepository communityRepository, BuildingRepository buildingRepository, UnitRepository unitRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.accessRepository = accessRepository;
        this.communityRepository = communityRepository;
        this.buildingRepository = buildingRepository;
        this.unitRepository = unitRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            log.info("Seeding initial SCOS roles and default data...");
            seedRoles();
        }

        if (permissionRepository.count() == 0) {
            log.info("Seeding initial SCOS permissions and linking to roles...");
            seedPermissionsAndRbac();
        }

        if (communityRepository.count() == 0) {
            seedCommunityAndInfrastructure();
        }

        if (userRepository.count() == 0) {
            seedDefaultAdmin();
        }
    }

    private void seedPermissionsAndRbac() {
        List<PermissionEntity> permissions = List.of(
                new PermissionEntity(null, "SCREEN:DASHBOARD", "SCREEN", "Access Executive Operations Dashboard"),
                new PermissionEntity(null, "SCREEN:BUILDING_MATRIX", "SCREEN", "Access Interactive 2D Building Floor Matrix"),
                new PermissionEntity(null, "SCREEN:USER_DIRECTORY", "SCREEN", "Access Resident & Staff User Directory"),
                new PermissionEntity(null, "SCREEN:USER_CREATE", "SCREEN", "Invite and provision new user accounts"),
                new PermissionEntity(null, "SCREEN:ROLE_MANAGEMENT", "SCREEN", "Configure Role-Permission Access Matrix"),
                new PermissionEntity(null, "SCREEN:COMMUNITY_REGISTER", "SCREEN", "Register and onboard new housing societies"),
                new PermissionEntity(null, "SCREEN:GATE_SECURITY", "SCREEN", "Gatehouse Security & Visitor Terminal"),
                new PermissionEntity(null, "SCREEN:WORK_ORDERS", "SCREEN", "Maintenance & Facility Work Order Desk")
        );
        permissionRepository.saveAll(permissions);

        List<PermissionEntity> all = permissionRepository.findAll();
        Map<String, PermissionEntity> permMap = all.stream().collect(Collectors.toMap(PermissionEntity::getCode, p -> p));

        roleRepository.findByCode("SUPER_ADMIN").ifPresent(role -> {
            role.setPermissions(new HashSet<>(all));
            roleRepository.save(role);
        });

        roleRepository.findByCode("COMMUNITY_MANAGER").ifPresent(role -> {
            role.setPermissions(new HashSet<>(all));
            roleRepository.save(role);
        });

        roleRepository.findByCode("HOA_OFFICER").ifPresent(role -> {
            Set<PermissionEntity> set = new HashSet<>();
            addIfPresent(set, permMap, "SCREEN:DASHBOARD", "SCREEN:BUILDING_MATRIX", "SCREEN:USER_DIRECTORY", "SCREEN:WORK_ORDERS");
            role.setPermissions(set);
            roleRepository.save(role);
        });

        roleRepository.findByCode("OWNER").ifPresent(role -> {
            Set<PermissionEntity> set = new HashSet<>();
            addIfPresent(set, permMap, "SCREEN:DASHBOARD", "SCREEN:BUILDING_MATRIX", "SCREEN:WORK_ORDERS");
            role.setPermissions(set);
            roleRepository.save(role);
        });

        roleRepository.findByCode("TENANT").ifPresent(role -> {
            Set<PermissionEntity> set = new HashSet<>();
            addIfPresent(set, permMap, "SCREEN:DASHBOARD", "SCREEN:BUILDING_MATRIX", "SCREEN:WORK_ORDERS");
            role.setPermissions(set);
            roleRepository.save(role);
        });

        roleRepository.findByCode("SECURITY_GUARD").ifPresent(role -> {
            Set<PermissionEntity> set = new HashSet<>();
            addIfPresent(set, permMap, "SCREEN:GATE_SECURITY");
            role.setPermissions(set);
            roleRepository.save(role);
        });

        roleRepository.findByCode("TECHNICIAN").ifPresent(role -> {
            Set<PermissionEntity> set = new HashSet<>();
            addIfPresent(set, permMap, "SCREEN:WORK_ORDERS");
            role.setPermissions(set);
            roleRepository.save(role);
        });
    }

    private void addIfPresent(Set<PermissionEntity> target, Map<String, PermissionEntity> map, String... codes) {
        for (String c : codes) {
            if (map.containsKey(c)) {
                target.add(map.get(c));
            }
        }
    }

    private void seedRoles() {
        List<RoleEntity> roles = List.of(
                new RoleEntity(null, "SUPER_ADMIN", "Super Administrator", "System-wide admin", true),
                new RoleEntity(null, "COMMUNITY_MANAGER", "Community Manager", "Estate administrator", true),
                new RoleEntity(null, "HOA_OFFICER", "HOA Board Member", "Elected committee member", true),
                new RoleEntity(null, "OWNER", "Apartment Owner", "Registered landlord/owner", true),
                new RoleEntity(null, "TENANT", "Resident Tenant", "Primary leaseholder", true),
                new RoleEntity(null, "SECURITY_GUARD", "Security Guard", "Gatehouse operator", true),
                new RoleEntity(null, "TECHNICIAN", "Maintenance Technician", "Facility technician", true)
        );
        roleRepository.saveAll(roles);
    }

    private void seedCommunityAndInfrastructure() {
        CommunityEntity community = new CommunityEntity();
        community.setCode("EMERALD_GRAND");
        community.setName("Emerald Grand Heights");
        community.setLegalName("Emerald Grand Residents Welfare Association");
        community.setAddressLine1("100 Outer Ring Road");
        community.setCity("Bangalore");
        community.setStateProvince("Karnataka");
        community.setPostalCode("560103");
        community.setCountryCode("IND");
        community.setPhone("+918022334455");
        community.setEmail("manager@emeraldgrand.org");
        community.setCurrencyCode("INR");
        community.setStatus("ACTIVE");
        community = communityRepository.save(community);

        BuildingEntity towerA = new BuildingEntity();
        towerA.setCommunityId(community.getId());
        towerA.setCode("TOWER_A");
        towerA.setName("Tower A - Emerald Royal");
        towerA.setTotalFloors(12);
        towerA.setBasementFloors(1);
        towerA.setTotalUnits(48);
        towerA.setHasElevator(true);
        towerA.setConstructionStatus("COMPLETED");
        towerA = buildingRepository.save(towerA);

        List<UnitEntity> units = List.of(
                createUnit(community.getId(), towerA.getId(), "A-1201", 12, "3BHK", "OWNER_OCCUPIED", 1550, 4800),
                createUnit(community.getId(), towerA.getId(), "A-1202", 12, "3BHK", "TENANT_OCCUPIED", 1550, 4800),
                createUnit(community.getId(), towerA.getId(), "A-1203", 12, "2BHK", "VACANT", 1150, 3550),
                createUnit(community.getId(), towerA.getId(), "A-1204", 12, "2BHK", "UNDER_MAINTENANCE", 1150, 3550),
                createUnit(community.getId(), towerA.getId(), "A-1101", 11, "3BHK", "OWNER_OCCUPIED", 1550, 4800),
                createUnit(community.getId(), towerA.getId(), "A-1102", 11, "3BHK", "NOTICE_PERIOD", 1550, 4800),
                createUnit(community.getId(), towerA.getId(), "A-1103", 11, "2BHK", "TENANT_OCCUPIED", 1150, 3550),
                createUnit(community.getId(), towerA.getId(), "A-1104", 11, "2BHK", "VACANT", 1150, 3550),
                createUnit(community.getId(), towerA.getId(), "A-1001", 10, "3BHK", "VACANT", 1550, 4800),
                createUnit(community.getId(), towerA.getId(), "A-1002", 10, "3BHK", "OWNER_OCCUPIED", 1550, 4800),
                createUnit(community.getId(), towerA.getId(), "A-1003", 10, "2BHK", "OWNER_OCCUPIED", 1150, 3550),
                createUnit(community.getId(), towerA.getId(), "A-1004", 10, "2BHK", "OWNER_OCCUPIED", 1150, 3550)
        );
        unitRepository.saveAll(units);
    }

    private UnitEntity createUnit(UUID communityId, UUID buildingId, String unitNum, int floor, String type, String status, double sqft, double fee) {
        UnitEntity unit = new UnitEntity();
        unit.setCommunityId(communityId);
        unit.setBuildingId(buildingId);
        unit.setUnitNumber(unitNum);
        unit.setFloorNumber(floor);
        unit.setUnitType(type);
        unit.setCarpetAreaSqft(BigDecimal.valueOf(sqft));
        unit.setSuperBuiltupAreaSqft(BigDecimal.valueOf(sqft * 1.25));
        unit.setBalconyCount(1);
        unit.setBathroomCount(2);
        unit.setOccupancyStatus(status);
        unit.setFurnishingStatus("SEMI_FURNISHED");
        unit.setBaseMaintenanceCharge(BigDecimal.valueOf(fee));
        unit.setActive(true);
        return unit;
    }

    private void seedDefaultAdmin() {
        UserEntity admin = new UserEntity();
        admin.setUsername("admin");
        admin.setEmail("admin@scos.local");
        admin.setPhoneNumber("+919999900001");
        admin.setPasswordHash(passwordEncoder.encode("Password@123"));
        admin.setFirstName("System");
        admin.setLastName("Admin");
        admin.setActive(true);
        admin.setVerified(true);
        admin = userRepository.save(admin);

        CommunityEntity community = communityRepository.findByCode("EMERALD_GRAND").orElse(null);
        RoleEntity superAdminRole = roleRepository.findByCode("SUPER_ADMIN").orElse(null);

        if (community != null && superAdminRole != null) {
            UserCommunityAccessEntity access = new UserCommunityAccessEntity();
            access.setUser(admin);
            access.setCommunityId(community.getId());
            access.setRole(superAdminRole);
            access.setStatus("ACTIVE");
            accessRepository.save(access);
        }
        log.info("Default SCOS admin created: admin / Password@123");
    }
}
