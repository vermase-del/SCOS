package com.scos.modules.maintenance.service;

import com.scos.modules.maintenance.entity.WorkOrderStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkOrderStateMachineTest {
    private final WorkOrderStateMachine machine=new WorkOrderStateMachine();
    @Test void managerCanAssignOpenTicket() {
        assertDoesNotThrow(()->machine.validateTransition(WorkOrderStatus.OPEN,WorkOrderStatus.ASSIGNED,"COMMUNITY_MANAGER","dispatch"));
    }
    @Test void cannotSkipDirectlyFromOpenToClosed() {
        assertThrows(IllegalArgumentException.class,()->machine.validateTransition(WorkOrderStatus.OPEN,WorkOrderStatus.CLOSED,"COMMUNITY_MANAGER","done"));
    }
    @Test void holdRequiresReason() {
        assertThrows(IllegalArgumentException.class,()->machine.validateTransition(WorkOrderStatus.IN_PROGRESS,WorkOrderStatus.ON_HOLD,"TECHNICIAN"," "));
    }
    @Test void resolvedTicketCanBeClosedByResident() {
        assertDoesNotThrow(()->machine.validateTransition(WorkOrderStatus.RESOLVED,WorkOrderStatus.CLOSED,"TENANT","verified"));
    }
}
