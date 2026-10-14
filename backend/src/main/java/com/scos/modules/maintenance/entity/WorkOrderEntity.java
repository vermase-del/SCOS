package com.scos.modules.maintenance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scos_work_orders", uniqueConstraints = @UniqueConstraint(name = "uq_wo_community_ticket", columnNames = {"community_id", "ticket_number"}), indexes = {
    @Index(name = "idx_wo_community_status", columnList = "community_id,status"),
    @Index(name = "idx_wo_sla_due", columnList = "community_id,sla_due_at,sla_breached"),
    @Index(name = "idx_wo_asset_status", columnList = "community_id,asset_id,status"),
    @Index(name = "idx_wo_planned_window", columnList = "community_id,planned_start_at,planned_end_at"),
    @Index(name = "idx_wo_type_status", columnList = "community_id,work_order_type,status")
})
public class WorkOrderEntity {
    @Id @Column(nullable=false, updatable=false) private UUID id;
    @Column(name="community_id", nullable=false, updatable=false) private UUID communityId;
    @Column(name="ticket_number", nullable=false, length=32, updatable=false) private String ticketNumber;
    @Column(nullable=false, length=160) private String title;
    @Column(nullable=false, columnDefinition="TEXT") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private WorkOrderScope scope;
    @Column(name="building_id") private UUID buildingId;
    @Column(name="unit_id") private UUID unitId;
    @Column(name="location_details", length=200) private String locationDetails;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=40) private WorkOrderCategory category;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private WorkOrderPriority priority;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private WorkOrderStatus status;
    @Column(name="reported_by_user_id", nullable=false) private UUID reportedByUserId;
    @Column(name="assigned_technician_id") private UUID assignedTechnicianId;
    @Column(name="sla_policy_version", nullable=false, length=20) private String slaPolicyVersion = "v1";
    @Column(name="sla_due_at", nullable=false) private Instant slaDueAt;
    @Column(name="sla_breached", nullable=false) private boolean slaBreached;
    @Column(name="hold_reason", length=60) private String holdReason;
    @Column(name="hold_started_at") private Instant holdStartedAt;
    @Column(name="accumulated_hold_seconds", nullable=false) private long accumulatedHoldSeconds;
    @Column(name="total_cost", nullable=false, precision=12, scale=2) private BigDecimal totalCost = BigDecimal.ZERO;
    @Column(name="currency_code", nullable=false, length=3) private String currencyCode = "INR";
    @Version @Column(nullable=false) private long version;
    @Column(name="created_by", nullable=false, length=60, updatable=false) private String createdBy;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
    @Column(name="updated_by", nullable=false, length=60) private String updatedBy;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;

    // V6 Work order planning and asset operations additions
    @Enumerated(EnumType.STRING)
    @Column(name="work_order_type", nullable=false, length=30)
    private WorkOrderType workOrderType = WorkOrderType.REACTIVE;

    @Column(name="reason_code", length=40)
    private String reasonCode;

    @Column(name="department_code", length=50)
    private String departmentCode;

    @Column(name="planned_start_at")
    private Instant plannedStartAt;

    @Column(name="planned_end_at")
    private Instant plannedEndAt;

    @Column(name="actual_start_at")
    private Instant actualStartAt;

    @Column(name="resolved_at")
    private Instant resolvedAt;

    @Column(name="out_of_service", nullable=false)
    private boolean outOfService = false;

    @Column(name="service_type", length=40)
    private String serviceType;

    @Column(name="asset_id")
    private UUID assetId;

    @Column(name="external_reference", length=120)
    private String externalReference;

    @PrePersist void onCreate() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (createdBy == null) createdBy = "system";
        if (updatedBy == null) updatedBy = createdBy;
        if (totalCost == null) totalCost = BigDecimal.ZERO;
        if (workOrderType == null) workOrderType = WorkOrderType.REACTIVE;
    }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public UUID getCommunityId(){return communityId;} public void setCommunityId(UUID v){communityId=v;}
    public String getTicketNumber(){return ticketNumber;} public void setTicketNumber(String v){ticketNumber=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public WorkOrderScope getScope(){return scope;} public void setScope(WorkOrderScope v){scope=v;}
    public UUID getBuildingId(){return buildingId;} public void setBuildingId(UUID v){buildingId=v;}
    public UUID getUnitId(){return unitId;} public void setUnitId(UUID v){unitId=v;}
    public String getLocationDetails(){return locationDetails;} public void setLocationDetails(String v){locationDetails=v;}
    public WorkOrderCategory getCategory(){return category;} public void setCategory(WorkOrderCategory v){category=v;}
    public WorkOrderPriority getPriority(){return priority;} public void setPriority(WorkOrderPriority v){priority=v;}
    public WorkOrderStatus getStatus(){return status;} public void setStatus(WorkOrderStatus v){status=v;}
    public UUID getReportedByUserId(){return reportedByUserId;} public void setReportedByUserId(UUID v){reportedByUserId=v;}
    public UUID getAssignedTechnicianId(){return assignedTechnicianId;} public void setAssignedTechnicianId(UUID v){assignedTechnicianId=v;}
    public String getSlaPolicyVersion(){return slaPolicyVersion;} public void setSlaPolicyVersion(String v){slaPolicyVersion=v;}
    public Instant getSlaDueAt(){return slaDueAt;} public void setSlaDueAt(Instant v){slaDueAt=v;}
    public boolean isSlaBreached(){return slaBreached;} public void setSlaBreached(boolean v){slaBreached=v;}
    public String getHoldReason(){return holdReason;} public void setHoldReason(String v){holdReason=v;}
    public Instant getHoldStartedAt(){return holdStartedAt;} public void setHoldStartedAt(Instant v){holdStartedAt=v;}
    public long getAccumulatedHoldSeconds(){return accumulatedHoldSeconds;} public void setAccumulatedHoldSeconds(long v){accumulatedHoldSeconds=v;}
    public BigDecimal getTotalCost(){return totalCost;} public void setTotalCost(BigDecimal v){totalCost=v;}
    public String getCurrencyCode(){return currencyCode;} public void setCurrencyCode(String v){currencyCode=v;}
    public long getVersion(){return version;} public void setVersion(long v){version=v;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String v){updatedBy=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}

    public WorkOrderType getWorkOrderType(){return workOrderType;} public void setWorkOrderType(WorkOrderType v){workOrderType=v;}
    public String getReasonCode(){return reasonCode;} public void setReasonCode(String v){reasonCode=v;}
    public String getDepartmentCode(){return departmentCode;} public void setDepartmentCode(String v){departmentCode=v;}
    public Instant getPlannedStartAt(){return plannedStartAt;} public void setPlannedStartAt(Instant v){plannedStartAt=v;}
    public Instant getPlannedEndAt(){return plannedEndAt;} public void setPlannedEndAt(Instant v){plannedEndAt=v;}
    public Instant getActualStartAt(){return actualStartAt;} public void setActualStartAt(Instant v){actualStartAt=v;}
    public Instant getResolvedAt(){return resolvedAt;} public void setResolvedAt(Instant v){resolvedAt=v;}
    public boolean isOutOfService(){return outOfService;} public void setOutOfService(boolean v){outOfService=v;}
    public String getServiceType(){return serviceType;} public void setServiceType(String v){serviceType=v;}
    public UUID getAssetId(){return assetId;} public void setAssetId(UUID v){assetId=v;}
    public String getExternalReference(){return externalReference;} public void setExternalReference(String v){externalReference=v;}
}
