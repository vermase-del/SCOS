package com.scos.modules.maintenance.entity;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="scos_work_order_assignments", indexes=@Index(name="idx_wo_assignments_ticket",columnList="work_order_id,assigned_at"))
public class WorkOrderAssignmentEntity {
 @Id private UUID id; @Column(name="work_order_id",nullable=false,updatable=false) private UUID workOrderId; @Column(name="technician_id",nullable=false,updatable=false) private UUID technicianId; @Column(name="assigned_by_user_id",nullable=false,updatable=false) private UUID assignedByUserId; @Column(length=255,updatable=false) private String notes; @Column(name="assigned_at",nullable=false,updatable=false) private Instant assignedAt;
 @PrePersist void onCreate(){if(id==null)id=UUID.randomUUID();if(assignedAt==null)assignedAt=Instant.now();}
 public UUID getId(){return id;} public void setId(UUID v){id=v;} public UUID getWorkOrderId(){return workOrderId;} public void setWorkOrderId(UUID v){workOrderId=v;} public UUID getTechnicianId(){return technicianId;} public void setTechnicianId(UUID v){technicianId=v;} public UUID getAssignedByUserId(){return assignedByUserId;} public void setAssignedByUserId(UUID v){assignedByUserId=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;} public Instant getAssignedAt(){return assignedAt;} public void setAssignedAt(Instant v){assignedAt=v;}
}
