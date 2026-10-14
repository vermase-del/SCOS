package com.scos.modules.maintenance.service;

import com.scos.modules.maintenance.dto.AssetDto;
import com.scos.modules.maintenance.dto.CreateAssetRequest;
import com.scos.modules.maintenance.entity.*;
import com.scos.modules.maintenance.repository.AssetRepository;
import com.scos.modules.user.entity.UserCommunityAccessEntity;
import com.scos.modules.user.entity.UserEntity;
import com.scos.modules.user.repository.UserCommunityAccessRepository;
import com.scos.modules.user.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class AssetService {
    private final AssetRepository assets;
    private final UserRepository users;
    private final UserCommunityAccessRepository communityAccess;
    private final JdbcTemplate jdbc;

    public AssetService(AssetRepository assets, UserRepository users,
        UserCommunityAccessRepository communityAccess, JdbcTemplate jdbc) {
        this.assets=assets; this.users=users; this.communityAccess=communityAccess; this.jdbc=jdbc;
    }

    @Transactional
    public AssetDto create(UUID communityId, CreateAssetRequest req, String username) {
        UserEntity actor=requireUser(username);
        requireCommunityMember(actor.getId(),communityId);
        if(assets.existsByCommunityIdAndAssetCodeIgnoreCase(communityId,req.assetCode().trim()))
            throw new IllegalArgumentException("An asset with this code already exists in the community");
        validateLocation(req.buildingId(),req.unitId(),communityId);
        if(req.installedOn()!=null && req.warrantyExpiresOn()!=null && req.warrantyExpiresOn().isBefore(req.installedOn()))
            throw new IllegalArgumentException("Warranty expiry cannot be before the installation date");
        AssetEntity asset=new AssetEntity();
        asset.setCommunityId(communityId); asset.setAssetCode(req.assetCode().trim());
        asset.setName(req.name().trim()); asset.setDescription(req.description()); asset.setAssetType(req.assetType().trim().toUpperCase(Locale.ROOT));
        asset.setCriticality(req.criticality()==null?AssetCriticality.MEDIUM:req.criticality());
        asset.setStatus(AssetStatus.OPERATIONAL); asset.setBuildingId(req.buildingId()); asset.setUnitId(req.unitId());
        asset.setLocationDetails(req.locationDetails()); asset.setManufacturer(req.manufacturer()); asset.setModelNumber(req.modelNumber());
        asset.setSerialNumber(req.serialNumber()); asset.setInstalledOn(req.installedOn()); asset.setWarrantyExpiresOn(req.warrantyExpiresOn());
        asset.setNextMaintenanceAt(req.nextMaintenanceAt()); asset.setActive(true); asset.setCreatedBy(username); asset.setUpdatedBy(username);
        return map(assets.save(asset));
    }

    @Transactional(readOnly=true)
    public Page<AssetDto> list(UUID communityId, AssetStatus status, int page, int size, String username) {
        UserEntity actor=requireUser(username); requireCommunityMember(actor.getId(),communityId);
        Pageable pageable=PageRequest.of(Math.max(0,page),Math.max(1,Math.min(size,100)),Sort.by(Sort.Direction.ASC,"name"));
        Page<AssetEntity> result=status==null
            ? assets.findByCommunityIdAndActiveTrueOrderByNameAsc(communityId,pageable)
            : assets.findByCommunityIdAndActiveTrueAndStatusOrderByNameAsc(communityId,status,pageable);
        return result.map(this::map);
    }

    @Transactional(readOnly=true)
    public AssetDto get(UUID id, UUID communityId, String username) {
        UserEntity actor=requireUser(username); requireCommunityMember(actor.getId(),communityId);
        return assets.findByIdAndCommunityId(id,communityId).filter(AssetEntity::isActive)
            .map(this::map).orElseThrow(()->new NoSuchElementException("Asset not found"));
    }

    private void validateLocation(UUID buildingId, UUID unitId, UUID communityId) {
        if(buildingId!=null) {
            Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM scos_buildings WHERE id=? AND community_id=?",Integer.class,buildingId,communityId);
            if(count==null||count==0) throw new SecurityException("Building does not belong to the selected community");
        }
        if(unitId!=null) {
            Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM scos_units WHERE id=? AND community_id=?",Integer.class,unitId,communityId);
            if(count==null||count==0) throw new SecurityException("Unit does not belong to the selected community");
            if(buildingId!=null) {
                Integer match=jdbc.queryForObject("SELECT COUNT(*) FROM scos_units WHERE id=? AND community_id=? AND building_id=?",Integer.class,unitId,communityId,buildingId);
                if(match==null||match==0) throw new IllegalArgumentException("Unit does not belong to the selected building");
            }
        }
    }
    private UserEntity requireUser(String username) {
        return users.findByUsername(username).orElseThrow(()->new NoSuchElementException("Authenticated user not found"));
    }
    private void requireCommunityMember(UUID userId, UUID communityId) {
        boolean member=communityAccess.findByUserIdAndCommunityId(userId,communityId).stream().anyMatch(a->"ACTIVE".equals(a.getStatus()));
        if(!member) throw new SecurityException("User does not have active access to this community");
    }
    private AssetDto map(AssetEntity a) {
        return new AssetDto(a.getId(),a.getCommunityId(),a.getAssetCode(),a.getName(),a.getDescription(),a.getAssetType(),
            a.getStatus(),a.getCriticality(),a.getBuildingId(),a.getUnitId(),a.getLocationDetails(),a.getManufacturer(),
            a.getModelNumber(),a.getSerialNumber(),a.getInstalledOn(),a.getWarrantyExpiresOn(),a.getLastMaintenanceAt(),
            a.getNextMaintenanceAt(),a.isActive(),a.getVersion(),a.getCreatedAt(),a.getUpdatedAt());
    }
}
