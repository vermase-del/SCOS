package com.scos.modules.maintenance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scos.modules.maintenance.dto.*;
import com.scos.modules.maintenance.entity.AssetCriticality;
import com.scos.modules.maintenance.entity.AssetStatus;
import com.scos.modules.maintenance.service.AssetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AssetControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Principal principal;
    private UUID communityId;

    private AssetDto stubDto;
    private AssetDto stubStatusDto;
    private AssetSummaryDto stubSummary;

    @BeforeEach
    void setUp() {
        communityId = UUID.randomUUID();
        principal = () -> "admin";

        stubDto = new AssetDto(
            UUID.randomUUID(), communityId, "GEN-001", "Diesel Generator 250kVA",
            "Backup power generator", "ELECTRICAL", AssetStatus.OPERATIONAL,
            AssetCriticality.CRITICAL, null, null, "Service Yard", "Cummins",
            "QSB6.7", "SN-1234", LocalDate.of(2023, 1, 1), LocalDate.of(2026, 1, 1),
            null, null, true, 0, Instant.now(), Instant.now()
        );

        stubStatusDto = new AssetDto(
            stubDto.id(), communityId, "GEN-001", "Diesel Generator 250kVA",
            "Backup power generator", "ELECTRICAL", AssetStatus.UNDER_MAINTENANCE,
            AssetCriticality.CRITICAL, null, null, "Service Yard", "Cummins",
            "QSB6.7", "SN-1234", LocalDate.of(2023, 1, 1), LocalDate.of(2026, 1, 1),
            null, null, true, 0, Instant.now(), Instant.now()
        );

        stubSummary = new AssetSummaryDto(15, 12, 2, 1, 0, 4, 3);

        AssetService stubService = new AssetService(null, null, null, null, null) {
            @Override
            public AssetDto create(UUID commId, CreateAssetRequest req, String username) {
                return stubDto;
            }
            @Override
            public AssetDto updateStatus(UUID id, UUID commId, UpdateAssetStatusRequest req, String username) {
                return stubStatusDto;
            }
            @Override
            public AssetSummaryDto getSummary(UUID commId, String username) {
                return stubSummary;
            }
            @Override
            public Page<AssetDto> list(UUID commId, AssetStatus status, int page, int size, String username) {
                return new PageImpl<>(List.of(stubDto));
            }
        };

        AssetController controller = new AssetController(stubService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void createAsset_returns201() throws Exception {
        CreateAssetRequest req = new CreateAssetRequest(
            "GEN-001", "Diesel Generator 250kVA", "Backup power generator",
            "ELECTRICAL", AssetCriticality.CRITICAL, null, null,
            "Service Yard", "Cummins", "QSB6.7", "SN-1234",
            LocalDate.of(2023, 1, 1), LocalDate.of(2026, 1, 1), Instant.now()
        );

        mockMvc.perform(post("/api/v1/assets")
                .principal(principal)
                .param("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.assetCode").value("GEN-001"));
    }

    @Test
    void updateStatus_returnsUpdatedAsset() throws Exception {
        UUID assetId = stubDto.id();
        UpdateAssetStatusRequest req = new UpdateAssetStatusRequest(AssetStatus.UNDER_MAINTENANCE, "Scheduled servicing");

        mockMvc.perform(patch("/api/v1/assets/{id}/status", assetId)
                .principal(principal)
                .param("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("UNDER_MAINTENANCE"));
    }

    @Test
    void getSummary_returnsKpis() throws Exception {
        mockMvc.perform(get("/api/v1/assets/summary")
                .principal(principal)
                .param("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalAssets").value(15))
            .andExpect(jsonPath("$.data.operationalCount").value(12));
    }
}
