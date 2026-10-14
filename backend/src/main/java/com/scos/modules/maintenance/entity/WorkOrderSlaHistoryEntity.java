package com.scos.modules.maintenance.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="scos_work_order_sla_history", indexes=@Index(name="idx_wo_sla_history_ticket",columnList="work_order_id,created_at"))
public class WorkOrderSlaHistoryEntity {
    @Id private UUID id;
    @Column(name="work_order_id",nullable=false,updatable=false) private UUID workOrderId;
    @Column(name="event_type",nullable=false,length=40,updatable=false) private String eventType;
    @Column(name="original_due_at",nullable=false,updatable=false) private Instant originalDueAt;
    @Column(name="new_due_at",nullable=false,updatable=false) private Instant newDueAt;
    @Column(name="hold_duration_seconds",updatable=false) private Long holdDurationSeconds;
    @Column(length=255,updatable=false) private String reason;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @PrePersist void onCreate(){if(id==null)id=UUID.randomUUID();if(createdAt==null)createdAt=Instant.now();}
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public UUID getWorkOrderId(){return workOrderId;} public void setWorkOrderId(UUID v){workOrderId=v;}
    public String getEventType(){return eventType;} public void setEventType(String v){eventType=v;}
    public Instant getOriginalDueAt(){return originalDueAt;} public void setOriginalDueAt(Instant v){originalDueAt=v;}
    public Instant getNewDueAt(){return newDueAt;} public void setNewDueAt(Instant v){newDueAt=v;}
    public Long getHoldDurationSeconds(){return holdDurationSeconds;} public void setHoldDurationSeconds(Long v){holdDurationSeconds=v;}
    public String getReason(){return reason;} public void setReason(String v){reason=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
