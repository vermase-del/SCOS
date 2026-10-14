package com.scos.config;

import com.scos.modules.community.entity.BuildingEntity;
import com.scos.modules.community.entity.CommunityEntity;
import com.scos.modules.community.entity.UnitEntity;
import com.scos.modules.community.repository.BuildingRepository;
import com.scos.modules.community.repository.CommunityRepository;
import com.scos.modules.community.repository.UnitRepository;
import com.scos.modules.maintenance.entity.*;
import com.scos.modules.maintenance.repository.*;
import com.scos.modules.user.entity.PermissionEntity;
import com.scos.modules.user.entity.RoleEntity;
import com.scos.modules.user.entity.UserCommunityAccessEntity;
import com.scos.modules.user.entity.UserEntity;
import com.scos.modules.user.repository.PermissionRepository;
import com.scos.modules.user.repository.RoleRepository;
import com.scos.modules.user.repository.UserCommunityAccessRepository;
import com.scos.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
    private final AssetRepository assetRepository;
    private final AssetStatusHistoryRepository assetStatusHistoryRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderSequenceRepository sequenceRepository;
    private final WorkOrderActivityRepository activityRepository;
    private final WorkOrderCostLineRepository costLineRepository;
    private final WorkOrderFeedbackRepository feedbackRepository;

    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository,
            PermissionRepository permissionRepository, UserCommunityAccessRepository accessRepository,
            CommunityRepository communityRepository, BuildingRepository buildingRepository,
            UnitRepository unitRepository, PasswordEncoder passwordEncoder,
            AssetRepository assetRepository, AssetStatusHistoryRepository assetStatusHistoryRepository,
            WorkOrderRepository workOrderRepository, WorkOrderSequenceRepository sequenceRepository,
            WorkOrderActivityRepository activityRepository, WorkOrderCostLineRepository costLineRepository,
            WorkOrderFeedbackRepository feedbackRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.accessRepository = accessRepository;
        this.communityRepository = communityRepository;
        this.buildingRepository = buildingRepository;
        this.unitRepository = unitRepository;
        this.passwordEncoder = passwordEncoder;
        this.assetRepository = assetRepository;
        this.assetStatusHistoryRepository = assetStatusHistoryRepository;
        this.workOrderRepository = workOrderRepository;
        this.sequenceRepository = sequenceRepository;
        this.activityRepository = activityRepository;
        this.costLineRepository = costLineRepository;
        this.feedbackRepository = feedbackRepository;
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

        seedOperationalMaintenanceData();
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
                new PermissionEntity(null, "SCREEN:WORK_ORDERS", "SCREEN", "Maintenance & Facility Work Order Desk"),
                new PermissionEntity(null, "WORKORDER:READ", "OPERATION", "View work orders and maintenance records"),
                new PermissionEntity(null, "WORKORDER:WRITE", "OPERATION", "Create and update work orders"),
                new PermissionEntity(null, "ASSET:READ", "OPERATION", "View asset register and equipment records"),
                new PermissionEntity(null, "ASSET:WRITE", "OPERATION", "Manage asset register and schedules")
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
            addIfPresent(set, permMap, "SCREEN:DASHBOARD", "SCREEN:BUILDING_MATRIX", "SCREEN:USER_DIRECTORY", "SCREEN:WORK_ORDERS", "WORKORDER:READ", "ASSET:READ");
            role.setPermissions(set);
            roleRepository.save(role);
        });

        roleRepository.findByCode("OWNER").ifPresent(role -> {
            Set<PermissionEntity> set = new HashSet<>();
            addIfPresent(set, permMap, "SCREEN:DASHBOARD", "SCREEN:BUILDING_MATRIX", "SCREEN:WORK_ORDERS", "WORKORDER:READ", "WORKORDER:WRITE");
            role.setPermissions(set);
            roleRepository.save(role);
        });

        roleRepository.findByCode("TENANT").ifPresent(role -> {
            Set<PermissionEntity> set = new HashSet<>();
            addIfPresent(set, permMap, "SCREEN:DASHBOARD", "SCREEN:BUILDING_MATRIX", "SCREEN:WORK_ORDERS", "WORKORDER:READ", "WORKORDER:WRITE");
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
            addIfPresent(set, permMap, "SCREEN:WORK_ORDERS", "WORKORDER:READ", "WORKORDER:WRITE", "ASSET:READ", "ASSET:WRITE");
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

    private void seedOperationalMaintenanceData() {
        CommunityEntity community = communityRepository.findByCode("EMERALD_GRAND").orElse(null);
        if (community == null) return;
        UUID communityId = community.getId();

        // 1. Seed Technician user (technician.ramesh) if missing
        UserEntity technician = userRepository.findByUsername("technician.ramesh").orElseGet(() -> {
            UserEntity tech = new UserEntity();
            tech.setUsername("technician.ramesh");
            tech.setEmail("technician.ramesh@emeraldgrand.org");
            tech.setPhoneNumber("+919988776655");
            tech.setPasswordHash(passwordEncoder.encode("Password@123"));
            tech.setFirstName("Ramesh");
            tech.setLastName("Kumar");
            tech.setActive(true);
            tech.setVerified(true);
            tech = userRepository.save(tech);

            RoleEntity techRole = roleRepository.findByCode("TECHNICIAN").orElse(null);
            if (techRole != null) {
                UserCommunityAccessEntity access = new UserCommunityAccessEntity();
                access.setUser(tech);
                access.setCommunityId(communityId);
                access.setRole(techRole);
                access.setStatus("ACTIVE");
                accessRepository.save(access);
            }
            log.info("Test technician created: technician.ramesh / Password@123");
            return tech;
        });

        UserEntity admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) return;

        BuildingEntity towerA = buildingRepository.findByCommunityIdAndCode(communityId, "TOWER_A").orElse(null);
        UUID buildingId = towerA != null ? towerA.getId() : null;
        UnitEntity unit1201 = buildingId != null ? unitRepository.findByBuildingIdAndUnitNumber(buildingId, "A-1201").orElse(null) : null;
        UnitEntity unit1002 = buildingId != null ? unitRepository.findByBuildingIdAndUnitNumber(buildingId, "A-1002").orElse(null) : null;

        // 2. Seed Assets if not present
        if (assetRepository.count() == 0) {
            log.info("Seeding community operational assets...");
            AssetEntity pump = createAsset(communityId, "PUMP-001", "Main Water Booster Pump A",
                "Primary hydrostatic booster pump for Tower A domestic water supply",
                "PLUMBING", AssetStatus.OPERATIONAL, AssetCriticality.CRITICAL, buildingId, null,
                "Tower A Basement Pump Room", "Kirloskar Brothers", "KBL-50-200", "KB2023-9941",
                LocalDate.of(2023, 5, 10), LocalDate.of(2026, 5, 10), Instant.now().minus(45, ChronoUnit.DAYS),
                Instant.now().plus(45, ChronoUnit.DAYS), "system");

            AssetEntity elevator = createAsset(communityId, "ELEV-001", "Passenger Elevator 1 - Tower A",
                "13-passenger traction lift servicing Basement to 12th Floor",
                "ELEVATOR", AssetStatus.OPERATIONAL, AssetCriticality.HIGH, buildingId, null,
                "Tower A Elevator Core 1", "Otis India", "Gen2-Comfort", "OTIS-2022-8114",
                LocalDate.of(2022, 11, 15), LocalDate.of(2027, 11, 15), Instant.now().minus(80, ChronoUnit.DAYS),
                Instant.now().plus(10, ChronoUnit.DAYS), "system");

            AssetEntity generator = createAsset(communityId, "GEN-001", "Diesel Generator 250kVA",
                "Emergency backup generator for lifts, common lighting and water pumps",
                "ELECTRICAL", AssetStatus.OPERATIONAL, AssetCriticality.CRITICAL, buildingId, null,
                "Service Yard Utility Enclosure", "Cummins India", "QSB6.7-G12", "CUM-2021-309",
                LocalDate.of(2021, 8, 20), LocalDate.of(2026, 8, 20), Instant.now().minus(15, ChronoUnit.DAYS),
                Instant.now().plus(75, ChronoUnit.DAYS), "system");

            AssetEntity cctv = createAsset(communityId, "CCTV-001", "Gatehouse CCTV Server & DVR",
                "32-channel NVR with continuous 30-day perimeter recording",
                "SECURITY_DEVICE", AssetStatus.OPERATIONAL, AssetCriticality.MEDIUM, null, null,
                "Main Gate Security Cabin", "Hikvision", "DS-9632NI-I8", "HK-2023-0182",
                LocalDate.of(2023, 1, 10), LocalDate.of(2026, 1, 10), Instant.now().minus(60, ChronoUnit.DAYS),
                Instant.now().plus(30, ChronoUnit.DAYS), "system");

            AssetEntity chiller = createAsset(communityId, "HVAC-001", "Clubhouse Central HVAC Chiller",
                "Variable refrigerant flow air-handling chiller for clubhouse",
                "HVAC", AssetStatus.OPERATIONAL, AssetCriticality.MEDIUM, null, null,
                "Clubhouse Terrace Plant Room", "Daikin", "VRV-IV-X", "DKN-2023-5510",
                LocalDate.of(2023, 3, 1), LocalDate.of(2028, 3, 1), Instant.now().minus(90, ChronoUnit.DAYS),
                Instant.now().plus(5, ChronoUnit.DAYS), "system");
        }

        // 3. Seed Work Orders if not present
        if (workOrderRepository.count() == 0) {
            log.info("Seeding operational work orders across lifecycle states...");
            Instant now = Instant.now();

            AssetEntity elevator = assetRepository.existsByCommunityIdAndAssetCodeIgnoreCase(communityId, "ELEV-001")
                ? assetRepository.findByCommunityIdAndActiveTrueOrderByNameAsc(communityId, org.springframework.data.domain.PageRequest.of(0, 10))
                    .stream().filter(a -> "ELEV-001".equals(a.getAssetCode())).findFirst().orElse(null) : null;
            AssetEntity generator = assetRepository.existsByCommunityIdAndAssetCodeIgnoreCase(communityId, "GEN-001")
                ? assetRepository.findByCommunityIdAndActiveTrueOrderByNameAsc(communityId, org.springframework.data.domain.PageRequest.of(0, 10))
                    .stream().filter(a -> "GEN-001".equals(a.getAssetCode())).findFirst().orElse(null) : null;
            AssetEntity pump = assetRepository.existsByCommunityIdAndAssetCodeIgnoreCase(communityId, "PUMP-001")
                ? assetRepository.findByCommunityIdAndActiveTrueOrderByNameAsc(communityId, org.springframework.data.domain.PageRequest.of(0, 10))
                    .stream().filter(a -> "PUMP-001".equals(a.getAssetCode())).findFirst().orElse(null) : null;

            // Ticket 1: OPEN - Private Unit Plumbing Leak
            WorkOrderEntity wo1 = seedTicket(communityId, "WO-2026-0001", "Master Bathroom Pipe Joint Leak",
                "Seepage detected under the master bathroom sink vanity. Water leaking to floor.",
                WorkOrderScope.PRIVATE_UNIT, buildingId, unit1201 != null ? unit1201.getId() : null, "Unit A-1201 Master Bath",
                WorkOrderCategory.PLUMBING, WorkOrderPriority.HIGH, WorkOrderStatus.OPEN,
                admin.getId(), null, now.plus(12, ChronoUnit.HOURS), false, BigDecimal.ZERO, WorkOrderType.REACTIVE, null, false);

            // Ticket 2: ASSIGNED - Elevator Routine Safety Inspection
            WorkOrderEntity wo2 = seedTicket(communityId, "WO-2026-0002", "Passenger Elevator Q4 Safety Inspection",
                "Mandatory quarterly rope tension check, governor test, and cabin light audit.",
                WorkOrderScope.COMMON_AREA, buildingId, null, "Tower A Elevator Core 1",
                WorkOrderCategory.ELEVATOR, WorkOrderPriority.MEDIUM, WorkOrderStatus.ASSIGNED,
                admin.getId(), technician.getId(), now.plus(24, ChronoUnit.HOURS), false, BigDecimal.ZERO,
                WorkOrderType.PREVENTIVE, elevator != null ? elevator.getId() : null, false);

            // Ticket 3: IN_PROGRESS - Diesel Generator Oil & Filter Servicing
            WorkOrderEntity wo3 = seedTicket(communityId, "WO-2026-0003", "Diesel Generator 250kVA Periodic Filter Servicing",
                "Replacing primary fuel filter, lube oil, and testing automatic transfer switch (ATS).",
                WorkOrderScope.COMMON_AREA, buildingId, null, "Utility Enclosure",
                WorkOrderCategory.ELECTRICAL, WorkOrderPriority.MEDIUM, WorkOrderStatus.IN_PROGRESS,
                admin.getId(), technician.getId(), now.plus(18, ChronoUnit.HOURS), false, BigDecimal.valueOf(1850.00),
                WorkOrderType.PREVENTIVE, generator != null ? generator.getId() : null, false);
            wo3.setActualStartAt(now.minus(2, ChronoUnit.HOURS));
            workOrderRepository.save(wo3);
            addCostLine(wo3.getId(), CostType.MATERIALS, "Engine Oil 15W40 (20 Litres)", BigDecimal.valueOf(1), BigDecimal.valueOf(1200.00), admin.getId());
            addCostLine(wo3.getId(), CostType.MATERIALS, "Fleetguard Lube Filter Cartridge", BigDecimal.valueOf(1), BigDecimal.valueOf(650.00), admin.getId());

            // Ticket 4: ON_HOLD - Booster Pump Valve Replacement
            WorkOrderEntity wo4 = seedTicket(communityId, "WO-2026-0004", "Water Booster Pump A Flange Valve Replacement",
                "Main isolation butterfly valve broken, causing low pressure on floors 9-12. Pump isolated.",
                WorkOrderScope.COMMON_AREA, buildingId, null, "Tower A Basement Pump Room",
                WorkOrderCategory.PLUMBING, WorkOrderPriority.CRITICAL, WorkOrderStatus.ON_HOLD,
                admin.getId(), technician.getId(), now.plus(4, ChronoUnit.HOURS), false, BigDecimal.ZERO,
                WorkOrderType.EMERGENCY, pump != null ? pump.getId() : null, true);
            wo4.setHoldReason("Awaiting delivery of specialized butterfly valve");
            wo4.setHoldStartedAt(now.minus(1, ChronoUnit.HOURS));
            workOrderRepository.save(wo4);

            // Ticket 5: RESOLVED - AC Cooling Issue in Unit A-1002
            WorkOrderEntity wo5 = seedTicket(communityId, "WO-2026-0005", "Split AC Gas Top-up & Condenser Cleaning",
                "Resident reported poor cooling in living area. Cleaned condenser coils and recharged R32 gas.",
                WorkOrderScope.PRIVATE_UNIT, buildingId, unit1002 != null ? unit1002.getId() : null, "Unit A-1002 Living Room",
                WorkOrderCategory.HVAC, WorkOrderPriority.LOW, WorkOrderStatus.RESOLVED,
                admin.getId(), technician.getId(), now.minus(3, ChronoUnit.HOURS), false, BigDecimal.valueOf(850.00),
                WorkOrderType.REACTIVE, null, false);
            wo5.setActualStartAt(now.minus(5, ChronoUnit.HOURS));
            wo5.setResolvedAt(now.minus(1, ChronoUnit.HOURS));
            workOrderRepository.save(wo5);
            addCostLine(wo5.getId(), CostType.LABOR, "Technician Service & Gas Charge Labour", BigDecimal.valueOf(1), BigDecimal.valueOf(850.00), admin.getId());

            // Ticket 6: CLOSED - Hallway Lighting Replacement
            WorkOrderEntity wo6 = seedTicket(communityId, "WO-2026-0006", "Tower A 11th Floor Corridor LED Replacement",
                "Replaced 2 flickering 18W ceiling panel fixtures.",
                WorkOrderScope.COMMON_AREA, buildingId, null, "Tower A 11th Floor Corridor",
                WorkOrderCategory.ELECTRICAL, WorkOrderPriority.LOW, WorkOrderStatus.CLOSED,
                admin.getId(), technician.getId(), now.minus(48, ChronoUnit.HOURS), false, BigDecimal.valueOf(520.00),
                WorkOrderType.CORRECTIVE, null, false);
            wo6.setActualStartAt(now.minus(50, ChronoUnit.HOURS));
            wo6.setResolvedAt(now.minus(45, ChronoUnit.HOURS));
            workOrderRepository.save(wo6);
            addCostLine(wo6.getId(), CostType.MATERIALS, "Havells 18W Square LED Panel", BigDecimal.valueOf(2), BigDecimal.valueOf(260.00), admin.getId());

            WorkOrderFeedbackEntity fb = new WorkOrderFeedbackEntity();
            fb.setWorkOrderId(wo6.getId());
            fb.setResidentUserId(admin.getId());
            fb.setRating(5);
            fb.setFeedbackComments("Quickly fixed, bright and clean hallway now!");
            fb.setResolutionConfirmed(true);
            feedbackRepository.save(fb);

            // Seed sequence state
            WorkOrderSequenceEntity seq = new WorkOrderSequenceEntity();
            seq.setCommunityId(communityId);
            seq.setYear(2026);
            seq.setCurrentSequence(6);
            sequenceRepository.save(seq);

            log.info("Seeded 6 sample work orders in EMERALD_GRAND with initial sequence counter set to 6.");
        }
    }

    private AssetEntity createAsset(UUID communityId, String code, String name, String desc,
            String type, AssetStatus status, AssetCriticality crit, UUID bId, UUID uId,
            String location, String mfr, String model, String serial,
            LocalDate installed, LocalDate warranty, Instant lastMaint, Instant nextMaint, String user) {
        AssetEntity a = new AssetEntity();
        a.setCommunityId(communityId);
        a.setAssetCode(code);
        a.setName(name);
        a.setDescription(desc);
        a.setAssetType(type);
        a.setStatus(status);
        a.setCriticality(crit);
        a.setBuildingId(bId);
        a.setUnitId(uId);
        a.setLocationDetails(location);
        a.setManufacturer(mfr);
        a.setModelNumber(model);
        a.setSerialNumber(serial);
        a.setInstalledOn(installed);
        a.setWarrantyExpiresOn(warranty);
        a.setLastMaintenanceAt(lastMaint);
        a.setNextMaintenanceAt(nextMaint);
        a.setActive(true);
        a.setCreatedBy(user);
        a.setUpdatedBy(user);
        AssetEntity saved = assetRepository.save(a);

        AssetStatusHistoryEntity h = new AssetStatusHistoryEntity();
        h.setCommunityId(communityId);
        h.setAssetId(saved.getId());
        h.setFromStatus(null);
        h.setToStatus(status);
        h.setReason("Asset installed and verified in initial system commissioning");
        assetStatusHistoryRepository.save(h);
        return saved;
    }

    private WorkOrderEntity seedTicket(UUID communityId, String ticketNumber, String title, String desc,
            WorkOrderScope scope, UUID buildingId, UUID unitId, String location,
            WorkOrderCategory category, WorkOrderPriority priority, WorkOrderStatus status,
            UUID reporterId, UUID techId, Instant slaDueAt, boolean slaBreached, BigDecimal totalCost,
            WorkOrderType type, UUID assetId, boolean outOfService) {
        WorkOrderEntity wo = new WorkOrderEntity();
        wo.setCommunityId(communityId);
        wo.setTicketNumber(ticketNumber);
        wo.setTitle(title);
        wo.setDescription(desc);
        wo.setScope(scope);
        wo.setBuildingId(buildingId);
        wo.setUnitId(unitId);
        wo.setLocationDetails(location);
        wo.setCategory(category);
        wo.setPriority(priority);
        wo.setStatus(status);
        wo.setReportedByUserId(reporterId);
        wo.setAssignedTechnicianId(techId);
        wo.setSlaDueAt(slaDueAt);
        wo.setSlaBreached(slaBreached);
        wo.setTotalCost(totalCost);
        wo.setWorkOrderType(type);
        wo.setAssetId(assetId);
        wo.setOutOfService(outOfService);
        wo.setCreatedBy("system");
        wo.setUpdatedBy("system");
        wo = workOrderRepository.save(wo);

        WorkOrderActivityEntity a = new WorkOrderActivityEntity();
        a.setWorkOrderId(wo.getId());
        a.setAuthorUserId(reporterId);
        a.setActivityType(ActivityType.CREATED);
        a.setNotes("Work order filed: " + title);
        activityRepository.save(a);

        return wo;
    }

    private void addCostLine(UUID workOrderId, CostType type, String desc, BigDecimal qty, BigDecimal price, UUID user) {
        WorkOrderCostLineEntity c = new WorkOrderCostLineEntity();
        c.setWorkOrderId(workOrderId);
        c.setCostType(type);
        c.setItemDescription(desc);
        c.setQuantity(qty);
        c.setUnitPrice(price);
        c.setTotalAmount(qty.multiply(price).setScale(2, java.math.RoundingMode.HALF_UP));
        c.setCurrencyCode("INR");
        c.setRecordedByUserId(user);
        costLineRepository.save(c);
    }
}
