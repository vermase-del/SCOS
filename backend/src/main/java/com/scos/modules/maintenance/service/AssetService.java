package com.scos.modules.maintenance.service;

import com.scos.modules.maintenance.dto.*;
import com.scos.modules.maintenance.entity.*;
import com.scos.modules.maintenance.repository.AssetRepository;
import com.scos.modules.maintenance.repository.AssetStatusHistoryRepository;
import com.scos.modules.user.entity.UserCommunityAccessEntity;
import com.scos.modules.user.entity.UserEntity;
import com.scos.modules.user.repository.UserCommunityAccessRepository;
import com.scos.modules.user.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AssetService {
    private final AssetRepository assets;
    private final AssetStatusHistoryRepository statusHistory;
    private final UserRepository users;
    private final UserCommunityAccessRepository communityAccess;
    private final JdbcTemplate jdbc;

    public AssetService(AssetRepository assets, AssetStatusHistoryRepository statusHistory,
        UserRepository users, UserCommunityAccessRepository communityAccess, JdbcTemplate jdbc) {
        this.assets = assets;
        this.statusHistory = statusHistory;
        this.users = users;
        this.communityAccess = communityAccess;
        this.jdbc = jdbc;
    }

    @Transactional
    public AssetDto create(UUID communityId, CreateAssetRequest req, String username) {
        UserEntity actor = requireUser(username);
        requireCommunityMember(actor.getId(), communityId);
        if (assets.existsByCommunityIdAndAssetCodeIgnoreCase(communityId, req.assetCode().trim()))
            throw new IllegalArgumentException("An asset with this code already exists in the community");
        validateLocation(req.buildingId(), req.unitId(), communityId);
        if (req.installedOn() != null && req.warrantyExpiresOn() != null && req.warrantyExpiresOn().isBefore(req.installedOn()))
            throw new IllegalArgumentException("Warranty expiry cannot be before the installation date");

        AssetEntity asset = new AssetEntity();
        asset.setCommunityId(communityId);
        asset.setAssetCode(req.assetCode().trim());
        asset.setName(req.name().trim());
        asset.setDescription(req.description());
        asset.setAssetType(req.assetType().trim().toUpperCase(Locale.ROOT));
        asset.setCriticality(req.criticality() == null ? AssetCriticality.MEDIUM : req.criticality());
        asset.setStatus(AssetStatus.OPERATIONAL);
        asset.setBuildingId(req.buildingId());
        asset.setUnitId(req.unitId());
        asset.setLocationDetails(req.locationDetails());
        asset.setManufacturer(req.manufacturer());
        asset.setModelNumber(req.modelNumber());
        asset.setSerialNumber(req.serialNumber());
        asset.setInstalledOn(req.installedOn());
        asset.setWarrantyExpiresOn(req.warrantyExpiresOn());
        asset.setNextMaintenanceAt(req.nextMaintenanceAt());
        asset.setActive(true);
        asset.setCreatedBy(username);
        asset.setUpdatedBy(username);
        AssetEntity saved = assets.save(asset);

        // Record initial status history event
        AssetStatusHistoryEntity history = new AssetStatusHistoryEntity();
        history.setCommunityId(communityId);
        history.setAssetId(saved.getId());
        history.setFromStatus(null);
        history.setToStatus(AssetStatus.OPERATIONAL);
        history.setReason("Asset registered into inventory");
        history.setChangedByUserId(actor.getId());
        statusHistory.save(history);

        return map(saved);
    }

    @Transactional
    public AssetDto update(UUID id, UUID communityId, UpdateAssetRequest req, String username) {
        UserEntity actor = requireUser(username);
        requireCommunityMember(actor.getId(), communityId);
        AssetEntity asset = findScoped(id, communityId);
        validateLocation(req.buildingId(), req.unitId(), communityId);
        if (req.installedOn() != null && req.warrantyExpiresOn() != null && req.warrantyExpiresOn().isBefore(req.installedOn()))
            throw new IllegalArgumentException("Warranty expiry cannot be before the installation date");

        asset.setName(req.name().trim());
        asset.setDescription(req.description());
        asset.setAssetType(req.assetType().trim().toUpperCase(Locale.ROOT));
        if (req.criticality() != null) asset.setCriticality(req.criticality());
        asset.setBuildingId(req.buildingId());
        asset.setUnitId(req.unitId());
        asset.setLocationDetails(req.locationDetails());
        asset.setManufacturer(req.manufacturer());
        asset.setModelNumber(req.modelNumber());
        asset.setSerialNumber(req.serialNumber());
        asset.setInstalledOn(req.installedOn());
        asset.setWarrantyExpiresOn(req.warrantyExpiresOn());
        asset.setNextMaintenanceAt(req.nextMaintenanceAt());
        asset.setUpdatedBy(username);

        return map(assets.save(asset));
    }

    @Transactional
    public AssetDto updateStatus(UUID id, UUID communityId, UpdateAssetStatusRequest req, String username) {
        UserEntity actor = requireUser(username);
        requireCommunityMember(actor.getId(), communityId);
        AssetEntity asset = findScoped(id, communityId);
        AssetStatus from = asset.getStatus();
        if (from == req.status()) {
            return map(asset);
        }

        asset.setStatus(req.status());
        asset.setUpdatedBy(username);
        AssetEntity saved = assets.save(asset);

        AssetStatusHistoryEntity history = new AssetStatusHistoryEntity();
        history.setCommunityId(communityId);
        history.setAssetId(saved.getId());
        history.setFromStatus(from);
        history.setToStatus(req.status());
        history.setReason(req.reason() != null && !req.reason().isBlank() ? req.reason().trim() : "Operational status update");
        history.setChangedByUserId(actor.getId());
        statusHistory.save(history);

        return map(saved);
    }

    @Transactional(readOnly = true)
    public List<AssetStatusHistoryDto> getStatusHistory(UUID id, UUID communityId, String username) {
        UserEntity actor = requireUser(username);
        requireCommunityMember(actor.getId(), communityId);
        findScoped(id, communityId); // Ensure asset exists and belongs to community
        return statusHistory.findByCommunityIdAndAssetIdOrderByChangedAtDesc(communityId, id).stream()
            .map(h -> new AssetStatusHistoryDto(h.getId(), h.getAssetId(), h.getFromStatus(), h.getToStatus(), h.getReason(), h.getChangedByUserId(), h.getChangedAt()))
            .toList();
    }

    @Transactional(readOnly = true)
    public AssetSummaryDto getSummary(UUID communityId, String username) {
        UserEntity actor = requireUser(username);
        requireCommunityMember(actor.getId(), communityId);
        long total = assets.countByCommunityIdAndActiveTrue(communityId);
        long operational = assets.countByCommunityIdAndActiveTrueAndStatus(communityId, AssetStatus.OPERATIONAL);
        long underMaint = assets.countByCommunityIdAndActiveTrueAndStatus(communityId, AssetStatus.UNDER_MAINTENANCE);
        long outOfService = assets.countByCommunityIdAndActiveTrueAndStatus(communityId, AssetStatus.OUT_OF_SERVICE);
        long retired = assets.countByCommunityIdAndActiveTrueAndStatus(communityId, AssetStatus.RETIRED);
        long critical = assets.countByCommunityIdAndActiveTrueAndCriticality(communityId, AssetCriticality.CRITICAL);
        Instant upcomingThreshold = Instant.now().plus(14, ChronoUnit.DAYS);
        long upcoming = assets.countByCommunityIdAndActiveTrueAndNextMaintenanceAtLessThanEqual(communityId, upcomingThreshold);
        return new AssetSummaryDto(total, operational, underMaint, outOfService, retired, critical, upcoming);
    }

    @Transactional(readOnly = true)
    public Page<AssetDto> list(UUID communityId, AssetStatus status, int page, int size, String username) {
        UserEntity actor = requireUser(username);
        requireCommunityMember(actor.getId(), communityId);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.ASC, "name"));
        Page<AssetEntity> result = status == null
            ? assets.findByCommunityIdAndActiveTrueOrderByNameAsc(communityId, pageable)
            : assets.findByCommunityIdAndActiveTrueAndStatusOrderByNameAsc(communityId, status, pageable);
        return result.map(this::map);
    }

    @Transactional(readOnly = true)
    public Page<AssetDto> getDueForMaintenance(UUID communityId, Instant threshold, int page, int size, String username) {
        UserEntity actor = requireUser(username);
        requireCommunityMember(actor.getId(), communityId);
        Instant cutoff = threshold != null ? threshold : Instant.now().plus(14, ChronoUnit.DAYS);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        return assets.findByCommunityIdAndActiveTrueAndNextMaintenanceAtLessThanEqualOrderByNextMaintenanceAtAsc(communityId, cutoff, pageable)
            .map(this::map);
    }

    @Transactional(readOnly = true)
    public AssetDto get(UUID id, UUID communityId, String username) {
        UserEntity actor = requireUser(username);
        requireCommunityMember(actor.getId(), communityId);
        return map(findScoped(id, communityId));
    }

    private AssetEntity findScoped(UUID id, UUID communityId) {
        return assets.findByIdAndCommunityId(id, communityId).filter(AssetEntity::isActive)
            .orElseThrow(() -> new NoSuchElementException("Asset not found"));
    }

    private void validateLocation(UUID buildingId, UUID unitId, UUID communityId) {
        if (buildingId != null) {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM scos_buildings WHERE id=? AND community_id=?", Integer.class, buildingId, communityId);
            if (count == null || count == 0) throw new SecurityException("Building does not belong to the selected community");
        }
        if (unitId != null) {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM scos_units WHERE id=? AND community_id=?", Integer.class, unitId, communityId);
            if (count == null || count == 0) throw new SecurityException("Unit does not belong to the selected community");
            if (buildingId != null) {
                Integer match = jdbc.queryForObject("SELECT COUNT(*) FROM scos_units WHERE id=? AND community_id=? AND building_id=?", Integer.class, unitId, communityId, buildingId);
                if (match == null || match == 0) throw new IllegalArgumentException("Unit does not belong to the selected building");
            }
        }
    }

    private UserEntity requireUser(String username) {
        return users.findByUsername(username).orElseThrow(() -> new NoSuchElementException("Authenticated user not found"));
    }

    private void requireCommunityMember(UUID userId, UUID communityId) {
        boolean member = communityAccess.findByUserIdAndCommunityId(userId, communityId).stream().anyMatch(a -> "ACTIVE".equals(a.getStatus()));
        if (!member) throw new SecurityException("User does not have active access to this community");
    }

    private AssetDto map(AssetEntity a) {
        return new AssetDto(a.getId(), a.getCommunityId(), a.getAssetCode(), a.getName(), a.getDescription(), a.getAssetType(),
            a.getStatus(), a.getCriticality(), a.getBuildingId(), a.getUnitId(), a.getLocationDetails(), a.getManufacturer(),
            a.getModelNumber(), a.getSerialNumber(), a.getInstalledOn(), a.getWarrantyExpiresOn(), a.getLastMaintenanceAt(),
            a.getNextMaintenanceAt(), a.isActive(), a.getVersion(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
