package com.scos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scos.modules.community.entity.CommunityEntity;
import com.scos.modules.community.repository.CommunityRepository;
import com.scos.modules.maintenance.dto.AssignTechnicianRequest;
import com.scos.modules.maintenance.dto.CreateAssetRequest;
import com.scos.modules.maintenance.dto.CreateWorkOrderRequest;
import com.scos.modules.maintenance.entity.*;
import com.scos.modules.maintenance.repository.AssetRepository;
import com.scos.modules.maintenance.repository.WorkOrderRepository;
import com.scos.modules.user.entity.PermissionEntity;
import com.scos.modules.user.entity.RoleEntity;
import com.scos.modules.user.entity.UserCommunityAccessEntity;
import com.scos.modules.user.entity.UserEntity;
import com.scos.modules.user.repository.PermissionRepository;
import com.scos.modules.user.repository.RoleRepository;
import com.scos.modules.user.repository.UserCommunityAccessRepository;
import com.scos.modules.user.repository.UserRepository;
import com.scos.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SecurityAndTenantIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserCommunityAccessRepository accessRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private CommunityEntity communityAlpha;
    private CommunityEntity communityBeta;

    private UserEntity userAlphaManager;
    private UserEntity userAlphaResident;
    private UserEntity userBetaManager;

    private String tokenAlphaManager;
    private String tokenAlphaResident;
    private String tokenBetaManager;

    private WorkOrderEntity alphaWorkOrder;
    private AssetEntity alphaAsset;

    @BeforeEach
    void setUp() {
        // Ensure standard roles exist
        RoleEntity managerRole = roleRepository.findByCode("COMMUNITY_MANAGER").orElseGet(() ->
                roleRepository.save(new RoleEntity(null, "COMMUNITY_MANAGER", "Community Manager", "Mgr", true)));
        RoleEntity residentRole = roleRepository.findByCode("RESIDENT").orElseGet(() ->
                roleRepository.save(new RoleEntity(null, "RESIDENT", "Resident", "Res", true)));
        RoleEntity superAdminRole = roleRepository.findByCode("SUPER_ADMIN").orElseGet(() ->
                roleRepository.save(new RoleEntity(null, "SUPER_ADMIN", "Super Admin", "Admin", true)));

        // Permissions
        List<String> permCodes = List.of("SCREEN:WORK_ORDERS", "WORKORDER:READ", "WORKORDER:WRITE", "ASSET:READ", "ASSET:WRITE");
        for (String code : permCodes) {
            if (permissionRepository.findByCode(code).isEmpty()) {
                permissionRepository.save(new PermissionEntity(null, code, "OPERATION", code));
            }
        }

        // 1. Create Community Alpha
        communityAlpha = new CommunityEntity();
        communityAlpha.setCode("COMM_ALPHA_" + UUID.randomUUID().toString().substring(0, 8));
        communityAlpha.setName("Alpha Towers");
        communityAlpha.setLegalName("Alpha Towers RWA");
        communityAlpha.setAddressLine1("1 Alpha Blvd");
        communityAlpha.setCity("Bengaluru");
        communityAlpha.setStateProvince("KA");
        communityAlpha.setPostalCode("560001");
        communityAlpha.setCountryCode("IND");
        communityAlpha.setStatus("ACTIVE");
        communityAlpha = communityRepository.save(communityAlpha);

        // 2. Create Community Beta
        communityBeta = new CommunityEntity();
        communityBeta.setCode("COMM_BETA_" + UUID.randomUUID().toString().substring(0, 8));
        communityBeta.setName("Beta Heights");
        communityBeta.setLegalName("Beta Heights RWA");
        communityBeta.setAddressLine1("2 Beta Ave");
        communityBeta.setCity("Bengaluru");
        communityBeta.setStateProvince("KA");
        communityBeta.setPostalCode("560002");
        communityBeta.setCountryCode("IND");
        communityBeta.setStatus("ACTIVE");
        communityBeta = communityRepository.save(communityBeta);

        // 3. Create Users
        userAlphaManager = createUser("alpha.mgr." + UUID.randomUUID().toString().substring(0, 6));
        userAlphaResident = createUser("alpha.res." + UUID.randomUUID().toString().substring(0, 6));
        userBetaManager = createUser("beta.mgr." + UUID.randomUUID().toString().substring(0, 6));

        // 4. Assign Community Memberships
        assignAccess(userAlphaManager, communityAlpha.getId(), managerRole);
        assignAccess(userAlphaResident, communityAlpha.getId(), residentRole);
        assignAccess(userBetaManager, communityBeta.getId(), managerRole);

        // 5. Generate Real JWT Tokens
        tokenAlphaManager = jwtTokenProvider.generateToken(
                userAlphaManager.getId(), userAlphaManager.getUsername(),
                List.of("COMMUNITY_MANAGER"), permCodes, communityAlpha.getId());

        tokenAlphaResident = jwtTokenProvider.generateToken(
                userAlphaResident.getId(), userAlphaResident.getUsername(),
                List.of("RESIDENT"), List.of("SCREEN:WORK_ORDERS", "WORKORDER:READ", "ASSET:READ"), communityAlpha.getId());

        tokenBetaManager = jwtTokenProvider.generateToken(
                userBetaManager.getId(), userBetaManager.getUsername(),
                List.of("COMMUNITY_MANAGER"), permCodes, communityBeta.getId());

        // 6. Seed Work Order in Alpha
        alphaWorkOrder = new WorkOrderEntity();
        alphaWorkOrder.setId(UUID.randomUUID());
        alphaWorkOrder.setCommunityId(communityAlpha.getId());
        alphaWorkOrder.setTicketNumber("WO-ALPHA-01");
        alphaWorkOrder.setTitle("Elevator Service");
        alphaWorkOrder.setDescription("Passenger elevator maintenance");
        alphaWorkOrder.setScope(WorkOrderScope.COMMON_AREA);
        alphaWorkOrder.setCategory(WorkOrderCategory.ELEVATOR);
        alphaWorkOrder.setPriority(WorkOrderPriority.HIGH);
        alphaWorkOrder.setStatus(WorkOrderStatus.OPEN);
        alphaWorkOrder.setReportedByUserId(userAlphaManager.getId());
        alphaWorkOrder.setSlaDueAt(java.time.Instant.now().plus(24, java.time.temporal.ChronoUnit.HOURS));
        alphaWorkOrder = workOrderRepository.save(alphaWorkOrder);

        // 7. Seed Asset in Alpha
        alphaAsset = new AssetEntity();
        alphaAsset.setCommunityId(communityAlpha.getId());
        alphaAsset.setAssetCode("AST-ALP-001");
        alphaAsset.setName("Alpha Diesel Generator");
        alphaAsset.setAssetType("ELECTRICAL");
        alphaAsset.setStatus(AssetStatus.OPERATIONAL);
        alphaAsset.setActive(true);
        alphaAsset = assetRepository.save(alphaAsset);
    }

    private UserEntity createUser(String username) {
        UserEntity u = new UserEntity();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPhoneNumber("+9199" + UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 8));
        u.setPasswordHash(passwordEncoder.encode("Password@123"));
        u.setFirstName("Test");
        u.setLastName("User");
        u.setActive(true);
        u.setVerified(true);
        return userRepository.save(u);
    }

    private void assignAccess(UserEntity user, UUID communityId, RoleEntity role) {
        UserCommunityAccessEntity access = new UserCommunityAccessEntity();
        access.setUser(user);
        access.setCommunityId(communityId);
        access.setRole(role);
        access.setStatus("ACTIVE");
        accessRepository.save(access);
    }

    @Test
    @DisplayName("Security Gate 1: Anonymous requests cannot access protected operations")
    void testAnonymousRequestsBlocked() throws Exception {
        // GET Work Orders without token
        mockMvc.perform(get("/api/v1/work-orders")
                        .param("communityId", communityAlpha.getId().toString()))
                .andExpect(status().isForbidden());

        // POST Work Order without token
        mockMvc.perform(post("/api/v1/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // GET Assets without token
        mockMvc.perform(get("/api/v1/assets")
                        .param("communityId", communityAlpha.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security Gate 2: Cross-community data access is strictly forbidden")
    void testCrossCommunityAccessBlocked() throws Exception {
        // User Beta Manager attempts to read Work Orders belonging to Community Alpha
        mockMvc.perform(get("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + tokenBetaManager)
                        .param("communityId", communityAlpha.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        // User Beta Manager attempts to read Asset Summary belonging to Community Alpha
        mockMvc.perform(get("/api/v1/assets/summary")
                        .header("Authorization", "Bearer " + tokenBetaManager)
                        .param("communityId", communityAlpha.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // User Beta Manager attempts to fetch specific Work Order in Alpha
        mockMvc.perform(get("/api/v1/work-orders/" + alphaWorkOrder.getId())
                        .header("Authorization", "Bearer " + tokenBetaManager)
                        .param("communityId", communityAlpha.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security Gate 3: Header spoofing with X-Community-Id is rejected")
    void testHeaderSpoofingBlocked() throws Exception {
        // User Beta Manager attempts to spoof X-Community-Id to Alpha
        mockMvc.perform(get("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + tokenBetaManager)
                        .header("X-Community-Id", communityAlpha.getId().toString())
                        .param("communityId", communityAlpha.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security Gate 4: Cross-community write operations are blocked")
    void testCrossCommunityWriteBlocked() throws Exception {
        CreateWorkOrderRequest woReq = new CreateWorkOrderRequest(
                communityAlpha.getId(),
                "Malicious Ticket",
                "Attempting to write ticket in Alpha",
                WorkOrderScope.COMMON_AREA,
                null, null, null,
                WorkOrderCategory.ELECTRICAL,
                WorkOrderPriority.HIGH
        );

        // Beta user trying to create ticket in Alpha
        mockMvc.perform(post("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + tokenBetaManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(woReq)))
                .andExpect(status().isForbidden());

        CreateAssetRequest assetReq = new CreateAssetRequest(
                "AST-BETA-HACK", "Hack Asset", "Desc", "HVAC", AssetCriticality.HIGH,
                null, null, null, null, null, null, null, null, null
        );

        // Beta user trying to register asset in Alpha
        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + tokenBetaManager)
                        .param("communityId", communityAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assetReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security Gate 5: Read-only roles cannot invoke write operations")
    void testReadOnlyRoleCannotInvokeWrite() throws Exception {
        // Resident in Alpha has read permissions, but cannot assign technicians
        AssignTechnicianRequest assignReq = new AssignTechnicianRequest(userAlphaManager.getId(), "Notes");
        mockMvc.perform(patch("/api/v1/work-orders/" + alphaWorkOrder.getId() + "/assign")
                        .header("Authorization", "Bearer " + tokenAlphaResident)
                        .param("communityId", communityAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isForbidden());

        // Resident in Alpha cannot register assets
        CreateAssetRequest assetReq = new CreateAssetRequest(
                "AST-RES-FAIL", "Resident Asset", "Desc", "HVAC", AssetCriticality.LOW,
                null, null, null, null, null, null, null, null, null
        );
        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + tokenAlphaResident)
                        .param("communityId", communityAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assetReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security Gate 6: Legitimate intra-community requests succeed")
    void testLegitimateIntraCommunityAccessSucceeds() throws Exception {
        // Alpha Manager can read Alpha work orders
        mockMvc.perform(get("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + tokenAlphaManager)
                        .param("communityId", communityAlpha.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));

        // Alpha Manager can read Alpha assets
        mockMvc.perform(get("/api/v1/assets")
                        .header("Authorization", "Bearer " + tokenAlphaManager)
                        .param("communityId", communityAlpha.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Security Gate 7: Errors do not expose sensitive implementation details")
    void testErrorsDoNotExposeSensitiveDetails() throws Exception {
        // Request with non-existent work order ID in Alpha
        mockMvc.perform(get("/api/v1/work-orders/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenAlphaManager)
                        .param("communityId", communityAlpha.getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.message", not(containsString("org.hibernate"))))
                .andExpect(jsonPath("$.message", not(containsString("SQLException"))));
    }
}
