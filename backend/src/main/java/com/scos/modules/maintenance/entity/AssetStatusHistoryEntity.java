package com.scos.modules.maintenance.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scos_asset_status_history", indexes = {
    @Index(name = "idx_scos_asset_status_history_asset", columnList = "community_id,asset_id,changed_at")
})
public class AssetStatusHistoryEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "community_id", nullable = false, updatable = false)
    private UUID communityId;

    @Column(name = "asset_id", nullable = false, updatable = false)
    private UUID assetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30, updatable = false)
    private AssetStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30, updatable = false)
    private AssetStatus toStatus;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String reason;

    @Column(name = "changed_by_user_id", updatable = false)
    private UUID changedByUserId;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCommunityId() { return communityId; }
    public void setCommunityId(UUID communityId) { this.communityId = communityId; }

    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }

    public AssetStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(AssetStatus fromStatus) { this.fromStatus = fromStatus; }

    public AssetStatus getToStatus() { return toStatus; }
    public void setToStatus(AssetStatus toStatus) { this.toStatus = toStatus; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public UUID getChangedByUserId() { return changedByUserId; }
    public void setChangedByUserId(UUID changedByUserId) { this.changedByUserId = changedByUserId; }

    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }
}
