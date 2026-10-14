package com.scos.modules.maintenance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scos.modules.maintenance.dto.*;
import com.scos.modules.maintenance.entity.*;
import com.scos.modules.maintenance.service.WorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkOrderControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Principal principal;
    private UUID communityId;

    private WorkOrderDto stubCreatedDto;
    private WorkOrderSummaryDto stubSummaryDto;
    private WorkOrderDto stubAssignedDto;

    @BeforeEach
    void setUp() {
        communityId = UUID.randomUUID();
        principal = () -> "admin";

        stubCreatedDto = new WorkOrderDto(
            UUID.randomUUID(), communityId, "WO-2026-0001", "Main Line Leak",
            "Pressure drop in basement", WorkOrderScope.COMMON_AREA, null, null,
            "Basement", WorkOrderCategory.PLUMBING, WorkOrderPriority.HIGH,
            WorkOrderStatus.OPEN, UUID.randomUUID(), null, Instant.now(),
            false, BigDecimal.ZERO, "INR", 0, Instant.now(), Instant.now(),
            List.of(), List.of(), WorkOrderType.REACTIVE, null, null, null, null, null, null, false, null, null, null, null, null
        );

        stubSummaryDto = new WorkOrderSummaryDto(10, 3, 2, 4, 1, 5, 0, 4.8);

        stubAssignedDto = new WorkOrderDto(
            UUID.randomUUID(), communityId, "WO-2026-0001", "Main Line Leak",
            "Desc", WorkOrderScope.COMMON_AREA, null, null,
            "Basement", WorkOrderCategory.PLUMBING, WorkOrderPriority.HIGH,
            WorkOrderStatus.ASSIGNED, UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
            false, BigDecimal.ZERO, "INR", 0, Instant.now(), Instant.now(),
            List.of(), List.of(), WorkOrderType.REACTIVE, null, null, null, null, null, null, false, null, null, null, null, null
        );

        WorkOrderService stubService = new WorkOrderService(null, null, null, null, null, null, null, null, null, null, null, null, null, null) {
            @Override
            public WorkOrderDto createWorkOrder(CreateWorkOrderRequest req, String username) {
                return stubCreatedDto;
            }
            @Override
            public WorkOrderSummaryDto getSummary(UUID commId, String username) {
                return stubSummaryDto;
            }
            @Override
            public Page<WorkOrderDto> getWorkOrders(UUID commId, WorkOrderStatus status, WorkOrderPriority priority, WorkOrderType type, UUID assetId, int page, int size, String username) {
                return new PageImpl<>(List.of(stubCreatedDto));
            }
            @Override
            public WorkOrderDto assignTechnician(UUID id, UUID commId, AssignTechnicianRequest req, String username) {
                return stubAssignedDto;
            }
        };

        WorkOrderController controller = new WorkOrderController(stubService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void createWorkOrder_validRequest_returns201() throws Exception {
        CreateWorkOrderRequest req = new CreateWorkOrderRequest(
            communityId, "Main Line Leak", "Pressure drop in basement",
            WorkOrderScope.COMMON_AREA, null, null, "Basement",
            WorkOrderCategory.PLUMBING, WorkOrderPriority.HIGH
        );

        mockMvc.perform(post("/api/v1/work-orders")
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.ticketNumber").value("WO-2026-0001"));
    }

    @Test
    void getSummary_returnsKpis() throws Exception {
        mockMvc.perform(get("/api/v1/work-orders/summary")
                .principal(principal)
                .param("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalActive").value(10))
            .andExpect(jsonPath("$.data.averageRating").value(4.8));
    }

    @Test
    void listWorkOrders_withFilters_returnsPagedResults() throws Exception {
        mockMvc.perform(get("/api/v1/work-orders")
                .principal(principal)
                .param("communityId", communityId.toString())
                .param("status", "OPEN")
                .param("priority", "HIGH")
                .param("workOrderType", "REACTIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void assignTechnician_callsService() throws Exception {
        UUID woId = UUID.randomUUID();
        UUID techId = UUID.randomUUID();
        AssignTechnicianRequest req = new AssignTechnicianRequest(techId, "Take tools");

        mockMvc.perform(patch("/api/v1/work-orders/{id}/assign", woId)
                .principal(principal)
                .param("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ASSIGNED"));
    }
}
