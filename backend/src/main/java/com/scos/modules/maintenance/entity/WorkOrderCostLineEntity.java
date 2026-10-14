package com.scos.modules.maintenance.entity;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="scos_work_order_cost_lines", indexes=@Index(name="idx_wo_cost_lines_ticket",columnList="work_order_id,created_at"))
public class WorkOrderCostLineEntity {
 @Id private UUID id; @Column(name="work_order_id",nullable=false,updatable=false) private UUID workOrderId;
 @Enumerated(EnumType.STRING) @Column(name="cost_type",nullable=false,length=30,updatable=false) private CostType costType;
 @Column(name="item_description",nullable=false,length=160,updatable=false) private String itemDescription;
 @Column(nullable=false,precision=10,scale=2,updatable=false) private BigDecimal quantity;
 @Column(name="unit_price",nullable=false,precision=12,scale=2,updatable=false) private BigDecimal unitPrice;
 @Column(name="total_amount",nullable=false,precision=12,scale=2,updatable=false) private BigDecimal totalAmount;
 @Column(name="currency_code",nullable=false,length=3,updatable=false) private String currencyCode;
 @Column(name="recorded_by_user_id",nullable=false,updatable=false) private UUID recordedByUserId;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @PrePersist void onCreate(){if(id==null)id=UUID.randomUUID();if(createdAt==null)createdAt=Instant.now();}
 public UUID getId(){return id;} public void setId(UUID v){id=v;} public UUID getWorkOrderId(){return workOrderId;} public void setWorkOrderId(UUID v){workOrderId=v;} public CostType getCostType(){return costType;} public void setCostType(CostType v){costType=v;} public String getItemDescription(){return itemDescription;} public void setItemDescription(String v){itemDescription=v;} public BigDecimal getQuantity(){return quantity;} public void setQuantity(BigDecimal v){quantity=v;} public BigDecimal getUnitPrice(){return unitPrice;} public void setUnitPrice(BigDecimal v){unitPrice=v;} public BigDecimal getTotalAmount(){return totalAmount;} public void setTotalAmount(BigDecimal v){totalAmount=v;} public String getCurrencyCode(){return currencyCode;} public void setCurrencyCode(String v){currencyCode=v;} public UUID getRecordedByUserId(){return recordedByUserId;} public void setRecordedByUserId(UUID v){recordedByUserId=v;} public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
