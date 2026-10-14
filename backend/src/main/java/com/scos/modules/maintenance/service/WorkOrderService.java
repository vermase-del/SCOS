package com.scos.modules.maintenance.service;

import com.scos.modules.maintenance.dto.*;
import com.scos.modules.maintenance.entity.*;
import com.scos.modules.maintenance.repository.*;
import com.scos.modules.user.entity.UserCommunityAccessEntity;
import com.scos.modules.user.entity.UserEntity;
import com.scos.modules.user.repository.UserCommunityAccessRepository;
import com.scos.modules.user.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkOrderService {
    private final WorkOrderRepository workOrders;
    private final WorkOrderSequenceService sequences;
    private final WorkOrderActivityRepository activities;
    private final WorkOrderAssignmentRepository assignments;
    private final WorkOrderCostLineRepository costs;
    private final WorkOrderFeedbackRepository feedback;
    private final UserRepository users;
    private final UserCommunityAccessRepository communityAccess;
    private final SlaEngineService sla;
    private final WorkOrderStateMachine stateMachine;

    public WorkOrderService(WorkOrderRepository workOrders, WorkOrderSequenceService sequences,
            WorkOrderActivityRepository activities, WorkOrderAssignmentRepository assignments,
            WorkOrderCostLineRepository costs, WorkOrderFeedbackRepository feedback,
            UserRepository users, UserCommunityAccessRepository communityAccess,
            SlaEngineService sla, WorkOrderStateMachine stateMachine) {
        this.workOrders=workOrders; this.sequences=sequences; this.activities=activities;
        this.assignments=assignments; this.costs=costs; this.feedback=feedback;
        this.users=users; this.communityAccess=communityAccess; this.sla=sla; this.stateMachine=stateMachine;
    }

    @Transactional
    public WorkOrderDto createWorkOrder(CreateWorkOrderRequest req, String username) {
        UserEntity actor=requireUser(username);
        UUID communityId=Objects.requireNonNull(req.communityId(),"communityId is required");
        requireCommunityMember(actor.getId(),communityId);
        if(req.scope()==WorkOrderScope.PRIVATE_UNIT && req.unitId()==null)
            throw new IllegalArgumentException("unitId is required for PRIVATE_UNIT work orders");
        Instant now=Instant.now();
        WorkOrderEntity wo=new WorkOrderEntity();
        wo.setId(UUID.randomUUID()); wo.setCommunityId(communityId); wo.setTicketNumber(sequences.generateTicketNumber(communityId));
        wo.setTitle(req.title().trim()); wo.setDescription(req.description().trim()); wo.setScope(req.scope());
        wo.setBuildingId(req.buildingId()); wo.setUnitId(req.unitId()); wo.setLocationDetails(req.locationDetails());
        wo.setCategory(req.category()); wo.setPriority(req.priority()); wo.setStatus(WorkOrderStatus.OPEN);
        wo.setReportedByUserId(actor.getId()); wo.setSlaDueAt(sla.calculateTargetDueAt(req.priority(),now));
        wo.setCreatedBy(username); wo.setUpdatedBy(username); wo.setCreatedAt(now); wo.setUpdatedAt(now);
        wo=workOrders.save(wo);
        addActivity(wo,actor.getId(),ActivityType.CREATED,null,WorkOrderStatus.OPEN,"Work order created");
        return map(wo);
    }

    @Transactional(readOnly=true)
    public Page<WorkOrderDto> getWorkOrders(UUID communityId, WorkOrderStatus status, WorkOrderPriority priority, int page, int size, String username) {
        UserEntity actor=requireUser(username); requireCommunityMember(actor.getId(),communityId);
        Pageable pageable=PageRequest.of(Math.max(0,page),Math.max(1,Math.min(size,100)),Sort.by(Sort.Direction.DESC,"createdAt"));
        Page<WorkOrderEntity> result;
        if(status!=null && priority==null) result=workOrders.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId,status,pageable);
        else if(priority!=null && status==null) result=workOrders.findByCommunityIdAndPriorityOrderByCreatedAtDesc(communityId,priority,pageable);
        else if(status==null) result=workOrders.findByCommunityIdOrderByCreatedAtDesc(communityId,pageable);
        else result=workOrders.findByCommunityIdAndStatusAndPriorityOrderByCreatedAtDesc(communityId,status,priority,pageable);
        return result.map(this::map);
    }

    @Transactional(readOnly=true)
    public WorkOrderDto getWorkOrder(UUID id, UUID communityId, String username) {
        UserEntity actor=requireUser(username); requireCommunityMember(actor.getId(),communityId);
        return map(findScoped(id,communityId));
    }

    @Transactional
    public WorkOrderDto assignTechnician(UUID id, UUID communityId, AssignTechnicianRequest req, String username) {
        UserEntity actor=requireUser(username); requireCommunityMember(actor.getId(),communityId);
        WorkOrderEntity wo=findScoped(id,communityId);
        requireCommunityMember(req.technicianId(),communityId);
        WorkOrderStatus before=wo.getStatus();
        if(before!=WorkOrderStatus.OPEN && before!=WorkOrderStatus.ASSIGNED && before!=WorkOrderStatus.IN_PROGRESS)
            throw new IllegalArgumentException("Technician cannot be assigned in status "+before);
        wo.setAssignedTechnicianId(req.technicianId());
        if(before==WorkOrderStatus.OPEN) wo.setStatus(WorkOrderStatus.ASSIGNED);
        wo.setUpdatedBy(username);
        WorkOrderEntity saved=workOrders.save(wo);
        WorkOrderAssignmentEntity assignment=new WorkOrderAssignmentEntity(); assignment.setWorkOrderId(saved.getId());
        assignment.setTechnicianId(req.technicianId()); assignment.setAssignedByUserId(actor.getId()); assignment.setNotes(req.notes());
        assignments.save(assignment);
        addActivity(saved,actor.getId(),ActivityType.ASSIGNMENT,before,saved.getStatus(),
            req.notes()==null||req.notes().isBlank()?"Technician assigned":req.notes());
        return map(saved);
    }

    @Transactional
    public WorkOrderDto transitionStatus(UUID id, UUID communityId, TransitionStatusRequest req, String username) {
        UserEntity actor=requireUser(username); String role=roleFor(actor.getId(),communityId);
        WorkOrderEntity wo=findScoped(id,communityId); WorkOrderStatus before=wo.getStatus();
        stateMachine.validateTransition(before,req.targetStatus(),role,req.notes());
        Instant now=Instant.now();
        if(req.targetStatus()==WorkOrderStatus.ON_HOLD) { wo.setHoldReason(req.notes().trim()); sla.pauseSla(wo,now); }
        if(before==WorkOrderStatus.ON_HOLD && req.targetStatus()==WorkOrderStatus.IN_PROGRESS) sla.resumeSla(wo,now);
        wo.setStatus(req.targetStatus()); wo.setUpdatedBy(username);
        if(req.targetStatus()==WorkOrderStatus.RESOLVED) wo.setSlaBreached(sla.isBreached(wo,now));
        WorkOrderEntity saved=workOrders.save(wo);
        ActivityType type=req.targetStatus()==WorkOrderStatus.ON_HOLD?ActivityType.HOLD_PLACED:
            (before==WorkOrderStatus.ON_HOLD && req.targetStatus()==WorkOrderStatus.IN_PROGRESS?ActivityType.HOLD_RESUMED:ActivityType.STATUS_TRANSITION);
        addActivity(saved,actor.getId(),type,before,req.targetStatus(),req.notes()==null?"Status changed":req.notes());
        return map(saved);
    }

    @Transactional
    public WorkOrderCostLineDto addCostLine(UUID id, UUID communityId, AddCostLineRequest req, String username) {
        UserEntity actor=requireUser(username); requireCommunityMember(actor.getId(),communityId);
        WorkOrderEntity wo=findScoped(id,communityId);
        if(wo.getStatus()==WorkOrderStatus.CLOSED || wo.getStatus()==WorkOrderStatus.CANCELLED)
            throw new IllegalArgumentException("Costs cannot be added to a closed or cancelled work order");
        BigDecimal quantity=req.quantity(), unitPrice=req.unitPrice();
        BigDecimal total=quantity.multiply(unitPrice).setScale(2,java.math.RoundingMode.HALF_UP);
        WorkOrderCostLineEntity line=new WorkOrderCostLineEntity(); line.setWorkOrderId(wo.getId()); line.setCostType(req.costType());
        line.setItemDescription(req.itemDescription().trim()); line.setQuantity(quantity); line.setUnitPrice(unitPrice);
        line.setTotalAmount(total); line.setCurrencyCode(req.currencyCode()==null||req.currencyCode().isBlank()?"INR":req.currencyCode().toUpperCase(Locale.ROOT));
        line.setRecordedByUserId(actor.getId()); line=costs.save(line);
        wo.setTotalCost(wo.getTotalCost().add(total)); wo.setUpdatedBy(username); workOrders.save(wo);
        addActivity(wo,actor.getId(),ActivityType.COST_RECORDED,wo.getStatus(),wo.getStatus(),"Cost recorded: "+line.getItemDescription()+" ("+total+" "+line.getCurrencyCode()+")");
        return new WorkOrderCostLineDto(line.getId(),line.getCostType(),line.getItemDescription(),line.getQuantity(),line.getUnitPrice(),line.getTotalAmount(),line.getCurrencyCode(),line.getRecordedByUserId(),line.getCreatedAt());
    }

    @Transactional
    public WorkOrderDto submitFeedback(UUID id, UUID communityId, SubmitFeedbackRequest req, String username) {
        UserEntity actor=requireUser(username); requireCommunityMember(actor.getId(),communityId);
        WorkOrderEntity wo=findScoped(id,communityId);
        String actorRole=roleFor(actor.getId(),communityId);
        if(!actor.getId().equals(wo.getReportedByUserId()) && !Set.of("OWNER","TENANT","RESIDENT").contains(actorRole))
            throw new SecurityException("Only the reporter or a resident can submit resolution feedback");
        if(wo.getStatus()!=WorkOrderStatus.RESOLVED) throw new IllegalArgumentException("Feedback is accepted only after resolution");
        if(feedback.findByWorkOrderId(id).isPresent()) throw new IllegalArgumentException("Feedback has already been submitted");
        WorkOrderFeedbackEntity f=new WorkOrderFeedbackEntity(); f.setWorkOrderId(id); f.setResidentUserId(actor.getId()); f.setRating(req.rating());
        f.setFeedbackComments(req.feedbackComments()); f.setResolutionConfirmed(Boolean.TRUE.equals(req.resolutionConfirmed())); feedback.save(f);
        if(f.isResolutionConfirmed()) {
            stateMachine.validateTransition(WorkOrderStatus.RESOLVED,WorkOrderStatus.CLOSED,actorRole,"Resident verified resolution");
            wo.setStatus(WorkOrderStatus.CLOSED); wo.setUpdatedBy(username); wo=workOrders.save(wo);
            addActivity(wo,actor.getId(),ActivityType.VERIFIED_CLOSED,WorkOrderStatus.RESOLVED,WorkOrderStatus.CLOSED,"Resident verified resolution; rating "+req.rating()+"/5");
        } else {
            addActivity(wo,actor.getId(),ActivityType.NOTE_ADDED,WorkOrderStatus.RESOLVED,WorkOrderStatus.RESOLVED,"Resident did not confirm resolution; rating "+req.rating()+"/5");
        }
        return map(wo);
    }

    @Transactional(readOnly=true)
    public WorkOrderSummaryDto getSummary(UUID communityId,String username) {
        UserEntity actor=requireUser(username); requireCommunityMember(actor.getId(),communityId);
        long active=workOrders.countByCommunityIdAndStatusIn(communityId,List.of(WorkOrderStatus.OPEN,WorkOrderStatus.ASSIGNED,WorkOrderStatus.IN_PROGRESS,WorkOrderStatus.ON_HOLD));
        long open=workOrders.countByCommunityIdAndStatus(communityId,WorkOrderStatus.OPEN);
        long assigned=workOrders.countByCommunityIdAndStatus(communityId,WorkOrderStatus.ASSIGNED);
        long inProgress=workOrders.countByCommunityIdAndStatus(communityId,WorkOrderStatus.IN_PROGRESS);
        long onHold=workOrders.countByCommunityIdAndStatus(communityId,WorkOrderStatus.ON_HOLD);
        long resolved=workOrders.countByCommunityIdAndStatus(communityId,WorkOrderStatus.RESOLVED);
        long breached=workOrders.countByCommunityIdAndSlaBreachedTrueAndStatusNotIn(communityId,List.of(WorkOrderStatus.CLOSED,WorkOrderStatus.CANCELLED));
        double avg=feedback.averageRatingForCommunity(communityId).orElse(0.0);
        return new WorkOrderSummaryDto(active,open,assigned,inProgress,onHold,resolved,breached,avg);
    }

    private WorkOrderEntity findScoped(UUID id,UUID communityId) {
        return workOrders.findByIdAndCommunityId(id,communityId).orElseThrow(()->new NoSuchElementException("Work order not found"));
    }
    private UserEntity requireUser(String username) {
        return users.findByUsername(username).orElseThrow(()->new NoSuchElementException("Authenticated user not found"));
    }
    private void requireCommunityMember(UUID userId,UUID communityId) {
        boolean member=communityAccess.findByUserIdAndCommunityId(userId,communityId).stream().anyMatch(a->"ACTIVE".equals(a.getStatus()));
        if(!member) throw new SecurityException("User does not have active access to this community");
    }
    private String roleFor(UUID userId,UUID communityId) {
        return communityAccess.findByUserIdAndCommunityId(userId,communityId).stream().filter(a->"ACTIVE".equals(a.getStatus()))
            .map(UserCommunityAccessEntity::getRole).filter(Objects::nonNull).map(r->r.getCode()).findFirst()
            .orElseThrow(()->new SecurityException("No active role for this community"));
    }
    private void addActivity(WorkOrderEntity wo,UUID author,ActivityType type,WorkOrderStatus from,WorkOrderStatus to,String notes) {
        WorkOrderActivityEntity a=new WorkOrderActivityEntity(); a.setWorkOrderId(wo.getId()); a.setAuthorUserId(author); a.setActivityType(type);
        a.setFromStatus(from); a.setToStatus(to); a.setNotes(notes==null?"":notes); activities.save(a);
    }
    private WorkOrderDto map(WorkOrderEntity wo) {
        List<WorkOrderActivityDto> history=activities.findByWorkOrderIdOrderByCreatedAtAsc(wo.getId()).stream()
            .map(a->new WorkOrderActivityDto(a.getId(),a.getAuthorUserId(),a.getActivityType(),a.getFromStatus(),a.getToStatus(),a.getNotes(),a.getCreatedAt())).toList();
        List<WorkOrderCostLineDto> lineItems=costs.findByWorkOrderIdOrderByCreatedAtAsc(wo.getId()).stream()
            .map(c->new WorkOrderCostLineDto(c.getId(),c.getCostType(),c.getItemDescription(),c.getQuantity(),c.getUnitPrice(),c.getTotalAmount(),c.getCurrencyCode(),c.getRecordedByUserId(),c.getCreatedAt())).toList();
        return new WorkOrderDto(wo.getId(),wo.getCommunityId(),wo.getTicketNumber(),wo.getTitle(),wo.getDescription(),wo.getScope(),wo.getBuildingId(),wo.getUnitId(),wo.getLocationDetails(),wo.getCategory(),wo.getPriority(),wo.getStatus(),wo.getReportedByUserId(),wo.getAssignedTechnicianId(),wo.getSlaDueAt(),wo.isSlaBreached(),wo.getTotalCost(),wo.getCurrencyCode(),wo.getVersion(),wo.getCreatedAt(),wo.getUpdatedAt(),history,lineItems);
    }
}
