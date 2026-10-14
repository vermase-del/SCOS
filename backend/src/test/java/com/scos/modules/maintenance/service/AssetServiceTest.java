package com.scos.modules.maintenance.service;

import com.scos.modules.maintenance.dto.*;
import com.scos.modules.maintenance.entity.*;
import com.scos.modules.maintenance.repository.AssetRepository;
import com.scos.modules.maintenance.repository.AssetStatusHistoryRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock AssetRepository assets;
    @Mock AssetStatusHistoryRepository statusHistory;
    @Mock UserRepository users;
    @Mock UserCommunityAccessRepository communityAccess;

    private AssetService service;
    private UUID communityId;
    private UUID userId;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        communityId = UUID.randomUUID();
        userId = UUID.randomUUID();
        testUser = new UserEntity();
        testUser.setId(userId);
        testUser.setUsername("manager.alice");

        RoleEntity role = new RoleEntity();
        role.setCode("COMMUNITY_MANAGER");

        UserCommunityAccessEntity access = new UserCommunityAccessEntity();
        access.setUser(testUser);
        access.setCommunityId(communityId);
        access.setRole(role);
        access.setStatus("ACTIVE");

        when(users.findByUsername("manager.alice")).thenReturn(Optional.of(testUser));
        when(communityAccess.findByUserIdAndCommunityId(userId, communityId)).thenReturn(List.of(access));

        JdbcTemplate jdbc = new JdbcTemplate();
        service = new AssetService(assets, statusHistory, users, communityAccess, jdbc);
    }

    @Test
    void createAsset_success() {
        when(assets.existsByCommunityIdAndAssetCodeIgnoreCase(communityId, "PUMP-001")).thenReturn(false);
        when(assets.save(any(AssetEntity.class))).thenAnswer(i -> {
            AssetEntity a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        CreateAssetRequest req = new CreateAssetRequest(
            "PUMP-001", "Primary Water Pump", "Main pump",
            "PLUMBING", AssetCriticality.HIGH, null, null,
            "Pump room", "Kirloskar", "KB-100", "SN-999",
            LocalDate.of(2023, 1, 1), LocalDate.of(2026, 1, 1),
            Instant.now()
        );

        AssetDto dto = service.create(communityId, req, "manager.alice");

        assertNotNull(dto);
        assertEquals("PUMP-001", dto.assetCode());
        assertEquals(AssetStatus.OPERATIONAL, dto.status());
        verify(assets).save(any(AssetEntity.class));
        verify(statusHistory).save(any(AssetStatusHistoryEntity.class));
    }

    @Test
    void createAsset_duplicateCode_throwsException() {
        when(assets.existsByCommunityIdAndAssetCodeIgnoreCase(communityId, "PUMP-001")).thenReturn(true);

        CreateAssetRequest req = new CreateAssetRequest(
            "PUMP-001", "Primary Water Pump", "Main pump",
            "PLUMBING", AssetCriticality.HIGH, null, null,
            "Pump room", null, null, null, null, null, null
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.create(communityId, req, "manager.alice"));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void createAsset_warrantyBeforeInstall_throwsException() {
        when(assets.existsByCommunityIdAndAssetCodeIgnoreCase(communityId, "PUMP-001")).thenReturn(false);

        CreateAssetRequest req = new CreateAssetRequest(
            "PUMP-001", "Primary Water Pump", "Main pump",
            "PLUMBING", AssetCriticality.HIGH, null, null,
            "Pump room", null, null, null,
            LocalDate.of(2024, 1, 1), LocalDate.of(2023, 1, 1), null
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.create(communityId, req, "manager.alice"));
        assertTrue(ex.getMessage().contains("Warranty expiry cannot be before"));
    }

    @Test
    void updateStatus_recordsHistoryAndUpdatesEntity() {
        UUID assetId = UUID.randomUUID();
        AssetEntity asset = new AssetEntity();
        asset.setId(assetId);
        asset.setCommunityId(communityId);
        asset.setStatus(AssetStatus.OPERATIONAL);
        asset.setActive(true);

        when(assets.findByIdAndCommunityId(assetId, communityId)).thenReturn(Optional.of(asset));
        when(assets.save(any(AssetEntity.class))).thenAnswer(i -> i.getArgument(0));

        UpdateAssetStatusRequest req = new UpdateAssetStatusRequest(AssetStatus.UNDER_MAINTENANCE, "Emergency pipe burst");
        AssetDto result = service.updateStatus(assetId, communityId, req, "manager.alice");

        assertEquals(AssetStatus.UNDER_MAINTENANCE, result.status());
        verify(assets).save(asset);
        verify(statusHistory).save(argThat(h ->
            h.getFromStatus() == AssetStatus.OPERATIONAL &&
            h.getToStatus() == AssetStatus.UNDER_MAINTENANCE &&
            "Emergency pipe burst".equals(h.getReason())
        ));
    }

    @Test
    void getSummary_returnsAggregatedKpis() {
        when(assets.countByCommunityIdAndActiveTrue(communityId)).thenReturn(10L);
        when(assets.countByCommunityIdAndActiveTrueAndStatus(communityId, AssetStatus.OPERATIONAL)).thenReturn(7L);
        when(assets.countByCommunityIdAndActiveTrueAndStatus(communityId, AssetStatus.UNDER_MAINTENANCE)).thenReturn(2L);
        when(assets.countByCommunityIdAndActiveTrueAndStatus(communityId, AssetStatus.OUT_OF_SERVICE)).thenReturn(1L);
        when(assets.countByCommunityIdAndActiveTrueAndStatus(communityId, AssetStatus.RETIRED)).thenReturn(0L);
        when(assets.countByCommunityIdAndActiveTrueAndCriticality(communityId, AssetCriticality.CRITICAL)).thenReturn(3L);
        when(assets.countByCommunityIdAndActiveTrueAndNextMaintenanceAtLessThanEqual(eq(communityId), any())).thenReturn(4L);

        AssetSummaryDto summary = service.getSummary(communityId, "manager.alice");

        assertEquals(10L, summary.totalAssets());
        assertEquals(7L, summary.operationalCount());
        assertEquals(2L, summary.underMaintenanceCount());
        assertEquals(1L, summary.outOfServiceCount());
        assertEquals(3L, summary.criticalCount());
        assertEquals(4L, summary.upcomingMaintenanceCount());
    }
}
