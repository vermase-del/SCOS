package com.scos.modules.maintenance.service;

import com.scos.modules.maintenance.dto.*;
import com.scos.modules.maintenance.entity.*;
import com.scos.modules.maintenance.repository.*;
import com.scos.modules.user.entity.RoleEntity;
import com.scos.modules.user.entity.UserCommunityAccessEntity;
import com.scos.modules.user.entity.UserEntity;
import com.scos.modules.user.repository.UserCommunityAccessRepository;
import com.scos.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock WorkOrderRepository workOrders;
    @Mock WorkOrderSequenceRepository sequenceRepository;
    @Mock WorkOrderActivityRepository activities;
    @Mock WorkOrderAssignmentRepository assignments;
    @Mock WorkOrderCostLineRepository costs;
    @Mock WorkOrderFeedbackRepository feedback;
    @Mock WorkOrderSlaHistoryRepository slaHistory;
    @Mock AssetRepository assets;
    @Mock AssetStatusHistoryRepository assetStatusHistory;
    @Mock UserRepository users;
    @Mock UserCommunityAccessRepository communityAccess;

    private WorkOrderService service;
    private UUID communityId;
    private UUID userId;
    private UserEntity testUser;
    private UserCommunityAccessEntity activeAccess;

    @BeforeEach
    void setUp() {
        communityId = UUID.randomUUID();
        userId = UUID.randomUUID();
        testUser = new UserEntity();
        testUser.setId(userId);
        testUser.setUsername("manager.alice");

        RoleEntity role = new RoleEntity();
        role.setCode("COMMUNITY_MANAGER");

        activeAccess = new UserCommunityAccessEntity();
        activeAccess.setUser(testUser);
        activeAccess.setCommunityId(communityId);
        activeAccess.setRole(role);
        activeAccess.setStatus("ACTIVE");

        WorkOrderSequenceService sequences = new WorkOrderSequenceService(sequenceRepository) {
            @Override
            public String generateTicketNumber(UUID commId) {
                return "WO-2026-0001";
            }
        };
        SlaEngineService sla = new SlaEngineService();
        WorkOrderStateMachine stateMachine = new WorkOrderStateMachine();
        JdbcTemplate jdbcTemplate = new JdbcTemplate();

        service = new WorkOrderService(
            workOrders, sequences, activities, assignments, costs, feedback, slaHistory,
            assets, assetStatusHistory, users, communityAccess, sla, stateMachine, jdbcTemplate
        );
    }

    private void mockUserAndAccess() {
        when(users.findByUsername("manager.alice")).thenReturn(Optional.of(testUser));
        when(communityAccess.findByUserIdAndCommunityId(userId, communityId))
            .thenReturn(List.of(activeAccess));
    }

    @Test
    void createWorkOrder_success_commonArea() {
        mockUserAndAccess();
        when(workOrders.save(any(WorkOrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        CreateWorkOrderRequest req = new CreateWorkOrderRequest(
            communityId, "Corridor Light Broken", "Flickering lamp",
            WorkOrderScope.COMMON_AREA, null, null, "Building 1 Hallway",
            WorkOrderCategory.ELECTRICAL, WorkOrderPriority.HIGH
        );

        WorkOrderDto result = service.createWorkOrder(req, "manager.alice");

        assertNotNull(result);
        assertEquals("WO-2026-0001", result.ticketNumber());
        assertEquals(WorkOrderStatus.OPEN, result.status());
        assertEquals(WorkOrderType.REACTIVE, result.workOrderType());
        verify(workOrders).save(any(WorkOrderEntity.class));
        verify(activities).save(any(WorkOrderActivityEntity.class));
    }

    @Test
    void createWorkOrder_privateUnitWithoutUnitId_throwsException() {
        mockUserAndAccess();

        CreateWorkOrderRequest req = new CreateWorkOrderRequest(
            communityId, "Tap Leaking", "Sink tap",
            WorkOrderScope.PRIVATE_UNIT, null, null, "Kitchen",
            WorkOrderCategory.PLUMBING, WorkOrderPriority.MEDIUM
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.createWorkOrder(req, "manager.alice"));
        assertTrue(ex.getMessage().contains("unitId is required"));
    }

    @Test
    void createWorkOrder_withInvalidPlannedWindow_throwsException() {
        mockUserAndAccess();
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant end = Instant.now().plus(1, ChronoUnit.DAYS); // Before start!

        CreateWorkOrderRequest req = new CreateWorkOrderRequest(
            communityId, "Elevator Service", "Quarterly check",
            WorkOrderScope.COMMON_AREA, null, null, "Lift 1",
            WorkOrderCategory.ELEVATOR, WorkOrderPriority.LOW,
            WorkOrderType.PREVENTIVE, null, null, null, start, end, false, null, null
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.createWorkOrder(req, "manager.alice"));
        assertTrue(ex.getMessage().contains("Planned end date cannot be before"));
    }

    @Test
    void createWorkOrder_withAssetOutOfService_setsAssetStatus() {
        mockUserAndAccess();
        UUID assetId = UUID.randomUUID();
        AssetEntity asset = new AssetEntity();
        asset.setId(assetId);
        asset.setCommunityId(communityId);
        asset.setActive(true);
        asset.setStatus(AssetStatus.OPERATIONAL);

        when(assets.findByIdAndCommunityId(assetId, communityId)).thenReturn(Optional.of(asset));
        when(workOrders.save(any(WorkOrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        CreateWorkOrderRequest req = new CreateWorkOrderRequest(
            communityId, "Water Pump Tripping", "Main pump failure",
            WorkOrderScope.COMMON_AREA, null, null, "Basement",
            WorkOrderCategory.PLUMBING, WorkOrderPriority.CRITICAL,
            WorkOrderType.EMERGENCY, assetId, null, null, null, null, true, null, null
        );

        WorkOrderDto result = service.createWorkOrder(req, "manager.alice");

        assertNotNull(result);
        assertTrue(result.outOfService());
        assertEquals(AssetStatus.UNDER_MAINTENANCE, asset.getStatus());
        verify(assets).save(asset);
        verify(assetStatusHistory).save(any(AssetStatusHistoryEntity.class));
    }

    @Test
    void assignTechnician_updatesStatusAndHistory() {
        mockUserAndAccess();
        UUID woId = UUID.randomUUID();
        UUID techId = UUID.randomUUID();

        UserCommunityAccessEntity techAccess = new UserCommunityAccessEntity();
        techAccess.setStatus("ACTIVE");
        when(communityAccess.findByUserIdAndCommunityId(techId, communityId)).thenReturn(List.of(techAccess));

        WorkOrderEntity wo = new WorkOrderEntity();
        wo.setId(woId);
        wo.setCommunityId(communityId);
        wo.setStatus(WorkOrderStatus.OPEN);
        when(workOrders.findByIdAndCommunityId(woId, communityId)).thenReturn(Optional.of(wo));
        when(workOrders.save(any(WorkOrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        AssignTechnicianRequest req = new AssignTechnicianRequest(techId, "Please inspect urgently");
        WorkOrderDto result = service.assignTechnician(woId, communityId, req, "manager.alice");

        assertEquals(WorkOrderStatus.ASSIGNED, result.status());
        assertEquals(techId, wo.getAssignedTechnicianId());
        verify(assignments).save(any(WorkOrderAssignmentEntity.class));
        verify(activities).save(any(WorkOrderActivityEntity.class));
    }

    @Test
    void transitionStatus_toOnHold_pausesSla() {
        mockUserAndAccess();
        UUID woId = UUID.randomUUID();
        WorkOrderEntity wo = new WorkOrderEntity();
        wo.setId(woId);
        wo.setCommunityId(communityId);
        wo.setStatus(WorkOrderStatus.IN_PROGRESS);
        wo.setSlaDueAt(Instant.now().plus(8, ChronoUnit.HOURS));

        when(workOrders.findByIdAndCommunityId(woId, communityId)).thenReturn(Optional.of(wo));
        when(workOrders.save(any(WorkOrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        TransitionStatusRequest req = new TransitionStatusRequest(WorkOrderStatus.ON_HOLD, "Awaiting parts delivery");
        WorkOrderDto result = service.transitionStatus(woId, communityId, req, "manager.alice");

        assertEquals(WorkOrderStatus.ON_HOLD, result.status());
        assertNotNull(wo.getHoldStartedAt());
        assertEquals("Awaiting parts delivery", wo.getHoldReason());
        verify(slaHistory).save(any(WorkOrderSlaHistoryEntity.class));
    }

    @Test
    void transitionStatus_toResolved_restoresAssetToOperational() {
        mockUserAndAccess();
        UUID woId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        WorkOrderEntity wo = new WorkOrderEntity();
        wo.setId(woId);
        wo.setCommunityId(communityId);
        wo.setStatus(WorkOrderStatus.IN_PROGRESS);
        wo.setAssetId(assetId);
        wo.setOutOfService(true);
        wo.setTicketNumber("WO-2026-0005");
        wo.setSlaDueAt(Instant.now().plus(4, ChronoUnit.HOURS));

        AssetEntity asset = new AssetEntity();
        asset.setId(assetId);
        asset.setStatus(AssetStatus.UNDER_MAINTENANCE);

        when(workOrders.findByIdAndCommunityId(woId, communityId)).thenReturn(Optional.of(wo));
        when(workOrders.save(any(WorkOrderEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(assets.findByIdAndCommunityId(assetId, communityId)).thenReturn(Optional.of(asset));

        TransitionStatusRequest req = new TransitionStatusRequest(WorkOrderStatus.RESOLVED, "Repairs completed successfully");
        WorkOrderDto result = service.transitionStatus(woId, communityId, req, "manager.alice");

        assertEquals(WorkOrderStatus.RESOLVED, result.status());
        assertNotNull(wo.getResolvedAt());
        assertEquals(AssetStatus.OPERATIONAL, asset.getStatus());
        verify(assets).save(asset);
        verify(assetStatusHistory).save(any(AssetStatusHistoryEntity.class));
    }

    @Test
    void addCostLine_calculatesTotalAndAggregates() {
        mockUserAndAccess();
        UUID woId = UUID.randomUUID();
        WorkOrderEntity wo = new WorkOrderEntity();
        wo.setId(woId);
        wo.setCommunityId(communityId);
        wo.setStatus(WorkOrderStatus.IN_PROGRESS);
        wo.setTotalCost(BigDecimal.valueOf(100.00));

        when(workOrders.findByIdAndCommunityId(woId, communityId)).thenReturn(Optional.of(wo));
        when(costs.save(any(WorkOrderCostLineEntity.class))).thenAnswer(i -> i.getArgument(0));

        AddCostLineRequest req = new AddCostLineRequest(CostType.MATERIALS, "Ball valve", BigDecimal.valueOf(2), BigDecimal.valueOf(150.00), "INR");
        WorkOrderCostLineDto costDto = service.addCostLine(woId, communityId, req, "manager.alice");

        assertEquals(BigDecimal.valueOf(300.00).setScale(2), costDto.totalAmount());
        assertEquals(BigDecimal.valueOf(400.00).setScale(2), wo.getTotalCost());
        verify(costs).save(any(WorkOrderCostLineEntity.class));
    }
}
