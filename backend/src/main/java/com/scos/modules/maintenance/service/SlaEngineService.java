package com.scos.modules.maintenance.service;
import com.scos.modules.maintenance.entity.*; import org.springframework.stereotype.Service; import java.time.Duration; import java.time.Instant;
@Service public class SlaEngineService {
 public Instant calculateTargetDueAt(WorkOrderPriority priority, Instant creationTime){Duration d=switch(priority){case CRITICAL->Duration.ofHours(4);case HIGH->Duration.ofHours(12);case MEDIUM->Duration.ofHours(24);case LOW->Duration.ofHours(48);};return creationTime.plus(d);}
 public void pauseSla(WorkOrderEntity wo, Instant holdTime){if(wo.getHoldStartedAt()!=null)throw new IllegalStateException("SLA is already on hold");wo.setHoldStartedAt(holdTime);}
 public Instant resumeSla(WorkOrderEntity wo, Instant resumeTime){Instant held=wo.getHoldStartedAt();if(held==null)throw new IllegalStateException("SLA is not on hold");long seconds=Math.max(0,Duration.between(held,resumeTime).getSeconds());wo.setSlaDueAt(wo.getSlaDueAt().plusSeconds(seconds));wo.setAccumulatedHoldSeconds(wo.getAccumulatedHoldSeconds()+seconds);wo.setHoldStartedAt(null);wo.setHoldReason(null);return wo.getSlaDueAt();}
 public boolean isBreached(WorkOrderEntity wo,Instant now){return !wo.getStatus().equals(WorkOrderStatus.CLOSED)&&!wo.getStatus().equals(WorkOrderStatus.CANCELLED)&&now.isAfter(wo.getSlaDueAt());}
}
