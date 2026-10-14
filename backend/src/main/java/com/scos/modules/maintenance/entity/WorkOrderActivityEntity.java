package com.scos.modules.maintenance.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="scos_work_order_activities", indexes=@Index(name="idx_wo_activities_ticket", columnList="work_order_id,created_at"))
public class WorkOrderActivityEntity {
 @Id @Column(nullable=false,updatable=false) private UUID id;
 @Column(name="work_order_id",nullable=false,updatable=false) private UUID workOrderId;
 @Column(name="author_user_id",nullable=false,updatable=false) private UUID authorUserId;
 @Enumerated(EnumType.STRING) @Column(name="activity_type",nullable=false,length=40,updatable=false) private ActivityType activityType;
 @Enumerated(EnumType.STRING) @Column(name="from_status",length=30,updatable=false) private WorkOrderStatus fromStatus;
 @Enumerated(EnumType.STRING) @Column(name="to_status",length=30,updatable=false) private WorkOrderStatus toStatus;
 @Column(nullable=false,columnDefinition="TEXT",updatable=false) private String notes;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @PrePersist void onCreate(){if(id==null)id=UUID.randomUUID();if(createdAt==null)createdAt=Instant.now();}
 public UUID getId(){return id;} public void setId(UUID v){id=v;} public UUID getWorkOrderId(){return workOrderId;} public void setWorkOrderId(UUID v){workOrderId=v;} public UUID getAuthorUserId(){return authorUserId;} public void setAuthorUserId(UUID v){authorUserId=v;} public ActivityType getActivityType(){return activityType;} public void setActivityType(ActivityType v){activityType=v;} public WorkOrderStatus getFromStatus(){return fromStatus;} public void setFromStatus(WorkOrderStatus v){fromStatus=v;} public WorkOrderStatus getToStatus(){return toStatus;} public void setToStatus(WorkOrderStatus v){toStatus=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;} public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
