package com.scos.modules.maintenance.service;

import com.scos.modules.maintenance.entity.WorkOrderEntity;
import com.scos.modules.maintenance.entity.WorkOrderPriority;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class SlaEngineServiceTest {
    private final SlaEngineService service = new SlaEngineService();
    @Test void criticalTargetIsFourElapsedHours() {
        Instant start=Instant.parse("2026-10-15T00:00:00Z");
        assertEquals(start.plusSeconds(4*3600),service.calculateTargetDueAt(WorkOrderPriority.CRITICAL,start));
    }
    @Test void otherPrioritiesUseConfiguredElapsedDurations() {
        Instant start=Instant.parse("2026-10-15T00:00:00Z");
        assertEquals(start.plusSeconds(12*3600),service.calculateTargetDueAt(WorkOrderPriority.HIGH,start));
        assertEquals(start.plusSeconds(24*3600),service.calculateTargetDueAt(WorkOrderPriority.MEDIUM,start));
        assertEquals(start.plusSeconds(48*3600),service.calculateTargetDueAt(WorkOrderPriority.LOW,start));
    }
    @Test void resumeMovesDeadlineByExactHoldDuration() {
        WorkOrderEntity wo=new WorkOrderEntity();
        Instant due=Instant.parse("2026-10-16T00:00:00Z");
        Instant hold=Instant.parse("2026-10-15T12:00:00Z");
        Instant resume=hold.plusSeconds(5400);
        wo.setSlaDueAt(due); wo.setHoldStartedAt(hold);
        assertEquals(due.plusSeconds(5400),service.resumeSla(wo,resume));
        assertEquals(5400,wo.getAccumulatedHoldSeconds());
        assertNull(wo.getHoldStartedAt());
    }
}
