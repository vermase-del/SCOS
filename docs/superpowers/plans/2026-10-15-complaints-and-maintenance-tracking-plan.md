# Complaint & Maintenance Tracking (Work Orders Desk) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an enterprise-grade Complaint & Maintenance Tracking Subsystem (Work Orders Desk) in SCOS with multi-tenant isolation, deterministic state machine enforcement, authoritative server-side SLA calculations, immutable business timeline logging, itemized cost accounting, and a dual-mode Angular 18 Kanban/Table operations console.

**Architecture:** Domain-driven Spring Boot 3.3.4 module (`com.scos.modules.maintenance`) with PostgreSQL/Flyway schema migrations, optimistic locking (`@Version`), Spring Security RBAC (`SCREEN:WORK_ORDERS`, `WORKORDER:READ`, `WORKORDER:WRITE`), and an Angular 18 frontend featuring a dual Kanban/List view, operational KPI metrics cards, and an interactive slide-over workbench drawer.

**Tech Stack:** Java 21, Spring Boot 3.3.4, Spring Data JPA, Flyway, PostgreSQL 16.9 / H2 (dev profile), Angular 18, TypeScript, RxJS, Vanilla CSS tokens.

---

## Global Constraints

- Multi-tenant tenant isolation: Every table must carry `community_id` with foreign key and index.
- Monetary values: Must use `NUMERIC(12,2)` with currency code (default `'INR'`).
- Concurrency control: Optimistic locking via `version` column on `scos_work_orders`.
- Immutability: Business activity timeline (`scos_work_order_activities`) must be append-only.
- Timezone rules: All timestamps stored in UTC (`TIMESTAMPTZ`), rendered in user/community local time.
- Permission codes: Enforce `SCREEN:WORK_ORDERS`, `WORKORDER:READ`, `WORKORDER:WRITE`.
- Test coverage: Automated unit and integration tests are mandatory release gates for each task.

---

## Detailed Task Breakdown

### Task 1: Database Migration & Schema Creation

**Files:**
- Create: `d:/HMSREFERENCE/scos/backend/src/main/resources/db/migration/V5__init_work_orders_and_maintenance_desk.sql`

**Interfaces:**
- Consumes: `scos_communities(id)`, `scos_buildings(id)`, `scos_units(id)`, `scos_users(id)`
- Produces: Tables `scos_work_orders`, `scos_work_order_sequences`, `scos_work_order_activities`, `scos_work_order_assignments`, `scos_work_order_cost_lines`, `scos_work_order_feedback`, `scos_work_order_sla_history`.

- [ ] **Step 1: Write Flyway migration script `V5__init_work_orders_and_maintenance_desk.sql`**

```sql
-- V5: SCOS Work Orders & Maintenance Desk Schema
CREATE TABLE IF NOT EXISTS scos_work_order_sequences (
    community_id UUID NOT NULL REFERENCES scos_communities(id) ON DELETE CASCADE,
    year INT NOT NULL,
    current_sequence BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (community_id, year)
);

CREATE TABLE IF NOT EXISTS scos_work_orders (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES scos_communities(id) ON DELETE CASCADE,
    ticket_number VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    scope VARCHAR(20) NOT NULL CHECK (scope IN ('PRIVATE_UNIT', 'COMMON_AREA')),
    building_id UUID REFERENCES scos_buildings(id) ON DELETE SET NULL,
    unit_id UUID REFERENCES scos_units(id) ON DELETE SET NULL,
    location_details VARCHAR(200),
    category VARCHAR(40) NOT NULL,
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'RESOLVED', 'CLOSED', 'CANCELLED')),
    reported_by_user_id UUID NOT NULL REFERENCES scos_users(id),
    assigned_technician_id UUID REFERENCES scos_users(id),
    sla_policy_version VARCHAR(20) NOT NULL DEFAULT 'v1',
    sla_due_at TIMESTAMPTZ NOT NULL,
    sla_breached BOOLEAN NOT NULL DEFAULT FALSE,
    hold_reason VARCHAR(60),
    hold_started_at TIMESTAMPTZ,
    accumulated_hold_seconds BIGINT NOT NULL DEFAULT 0,
    total_cost NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'INR',
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(60) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(60) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wo_community_ticket UNIQUE (community_id, ticket_number)
);

CREATE INDEX IF NOT EXISTS idx_wo_community_status ON scos_work_orders(community_id, status);
CREATE INDEX IF NOT EXISTS idx_wo_sla_due ON scos_work_orders(community_id, sla_due_at, sla_breached);
CREATE INDEX IF NOT EXISTS idx_wo_technician ON scos_work_orders(assigned_technician_id, status);

CREATE TABLE IF NOT EXISTS scos_work_order_activities (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL REFERENCES scos_work_orders(id) ON DELETE CASCADE,
    author_user_id UUID NOT NULL REFERENCES scos_users(id),
    activity_type VARCHAR(40) NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30),
    notes TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wo_activities_ticket ON scos_work_order_activities(work_order_id, created_at);

CREATE TABLE IF NOT EXISTS scos_work_order_assignments (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL REFERENCES scos_work_orders(id) ON DELETE CASCADE,
    technician_id UUID NOT NULL REFERENCES scos_users(id),
    assigned_by_user_id UUID NOT NULL REFERENCES scos_users(id),
    notes VARCHAR(255),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS scos_work_order_cost_lines (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL REFERENCES scos_work_orders(id) ON DELETE CASCADE,
    cost_type VARCHAR(30) NOT NULL CHECK (cost_type IN ('LABOR', 'MATERIALS', 'VENDOR_FEE', 'EQUIPMENT', 'OTHER')),
    item_description VARCHAR(160) NOT NULL,
    quantity NUMERIC(10,2) NOT NULL DEFAULT 1.00,
    unit_price NUMERIC(12,2) NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'INR',
    recorded_by_user_id UUID NOT NULL REFERENCES scos_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS scos_work_order_feedback (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL UNIQUE REFERENCES scos_work_orders(id) ON DELETE CASCADE,
    resident_user_id UUID NOT NULL REFERENCES scos_users(id),
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    feedback_comments TEXT,
    resolution_confirmed BOOLEAN NOT NULL,
    is_auto_closed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS scos_work_order_sla_history (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL REFERENCES scos_work_orders(id) ON DELETE CASCADE,
    event_type VARCHAR(40) NOT NULL,
    original_due_at TIMESTAMPTZ NOT NULL,
    new_due_at TIMESTAMPTZ NOT NULL,
    hold_duration_seconds BIGINT,
    reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

- [ ] **Step 2: Commit migration file**

```bash
git add backend/src/main/resources/db/migration/V5__init_work_orders_and_maintenance_desk.sql
git commit -m "feat(maintenance): add V5 database migration for work orders and maintenance desk"
```

---

### Task 2: Domain Entities & Enums

**Files:**
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/entity/WorkOrderScope.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/entity/WorkOrderCategory.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/entity/WorkOrderPriority.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/entity/WorkOrderStatus.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/entity/CostType.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/entity/ActivityType.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/entity/WorkOrderSequenceEntity.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/entity/WorkOrderEntity.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/entity/WorkOrderActivityEntity.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/entity/WorkOrderAssignmentEntity.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/entity/WorkOrderCostLineEntity.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/entity/WorkOrderFeedbackEntity.java`

- [ ] **Step 1: Create Enums**
  - `WorkOrderScope`: `PRIVATE_UNIT`, `COMMON_AREA`
  - `WorkOrderPriority`: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`
  - `WorkOrderStatus`: `OPEN`, `ASSIGNED`, `IN_PROGRESS`, `ON_HOLD`, `RESOLVED`, `CLOSED`, `CANCELLED`
  - `WorkOrderCategory`: `PLUMBING`, `ELECTRICAL`, `HVAC`, `ELEVATOR`, `CIVIL`, `SECURITY_DEVICE`, `GARDENING`, `OTHER`
  - `CostType`: `LABOR`, `MATERIALS`, `VENDOR_FEE`, `EQUIPMENT`, `OTHER`
  - `ActivityType`: `STATUS_TRANSITION`, `ASSIGNMENT`, `NOTE_ADDED`, `HOLD_PLACED`, `HOLD_RESUMED`, `COST_RECORDED`, `VERIFIED_CLOSED`

- [ ] **Step 2: Create JPA Entities with `@Version` optimistic locking on `WorkOrderEntity`**
  - Implement full getters/setters, relations to `CommunityEntity`, `BuildingEntity`, `UnitEntity`, `UserEntity`.
  - Add `@PrePersist` and `@PreUpdate` auditing hooks for timestamps and user tracking.

- [ ] **Step 3: Test compilation**
  Run: `mvn compile -DskipTests` (inside `scos/backend`)
  Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**
  ```bash
  git add backend/src/main/java/com/scos/modules/maintenance/entity/
  git commit -m "feat(maintenance): add domain entities and enums for work orders"
  ```

---

### Task 3: Concurrency-Safe Sequence Counter & Repositories

**Files:**
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/repository/WorkOrderRepository.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/repository/WorkOrderSequenceRepository.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/repository/WorkOrderActivityRepository.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/repository/WorkOrderCostLineRepository.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/repository/WorkOrderFeedbackRepository.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/service/WorkOrderSequenceService.java`
- Test: `d:/HMSREFERENCE/scos/backend/src/test/java/com/scos/modules/maintenance/service/WorkOrderSequenceServiceTest.java`

**Interfaces:**
- Produces: `WorkOrderSequenceService.generateTicketNumber(UUID communityId): String` -> Format: `WO-YYYY-0001`

- [ ] **Step 1: Write Unit Test `WorkOrderSequenceServiceTest`**
  - Test increments sequence monotonically per community and year.
  - Test formats number with leading zeros (e.g. `WO-2026-0001`, `WO-2026-0002`).

- [ ] **Step 2: Implement `WorkOrderSequenceService`**
  - Use synchronized/transactional upsert with `PESSIMISTIC_WRITE` or atomic increment on `scos_work_order_sequences`.

- [ ] **Step 3: Run Test**
  Run: `mvn test -Dtest=WorkOrderSequenceServiceTest`
  Expected: PASS

- [ ] **Step 4: Commit**
  ```bash
  git add backend/src/main/java/com/scos/modules/maintenance/repository/ backend/src/main/java/com/scos/modules/maintenance/service/WorkOrderSequenceService.java backend/src/test/
  git commit -m "feat(maintenance): add sequence numbering service and repositories"
  ```

---

### Task 4: Authoritative SLA Calculation Engine & State Machine

**Files:**
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/service/SlaEngineService.java`
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/service/WorkOrderStateMachine.java`
- Test: `d:/HMSREFERENCE/scos/backend/src/test/java/com/scos/modules/maintenance/service/SlaEngineServiceTest.java`
- Test: `d:/HMSREFERENCE/scos/backend/src/test/java/com/scos/modules/maintenance/service/WorkOrderStateMachineTest.java`

**Interfaces:**
- Produces:
  - `SlaEngineService.calculateTargetDueAt(WorkOrderPriority priority, Instant creationTime): Instant`
  - `SlaEngineService.pauseSla(WorkOrderEntity wo, Instant holdTime): void`
  - `SlaEngineService.resumeSla(WorkOrderEntity wo, Instant resumeTime): Instant`
  - `SlaEngineService.isBreached(WorkOrderEntity wo, Instant currentTime): boolean`
  - `WorkOrderStateMachine.validateTransition(WorkOrderStatus from, WorkOrderStatus to, String roleCode, String notes): void`

- [ ] **Step 1: Write Unit Test for `SlaEngineServiceTest`**
  - Test `CRITICAL` targets: +4 hours (24/7 elapsed time).
  - Test `HIGH`: +12 hours.
  - Test `MEDIUM`: +24 hours.
  - Test `LOW`: +48 hours.
  - Test hold pause and resume shifts `slaDueAt` exactly by hold duration.

- [ ] **Step 2: Implement `SlaEngineService`**
  - Implement duration calculations and hold recalculations using `java.time.Instant`.

- [ ] **Step 3: Write Unit Test for `WorkOrderStateMachineTest`**
  - Test `OPEN` -> `ASSIGNED` allows `SUPER_ADMIN` and `COMMUNITY_MANAGER`.
  - Test `OPEN` -> `CLOSED` throws `InvalidStateTransitionException` (cannot jump straight to closed).
  - Test `IN_PROGRESS` -> `ON_HOLD` requires non-empty reason.
  - Test `RESOLVED` -> `CLOSED` allows resident confirmation.

- [ ] **Step 4: Implement `WorkOrderStateMachine`**
  - Guard transitions and throw explicit exceptions with clear HTTP 400 messages when violated.

- [ ] **Step 5: Run Tests**
  Run: `mvn test -Dtest=SlaEngineServiceTest,WorkOrderStateMachineTest`
  Expected: PASS

- [ ] **Step 6: Commit**
  ```bash
  git add backend/src/main/java/com/scos/modules/maintenance/service/SlaEngineService.java backend/src/main/java/com/scos/modules/maintenance/service/WorkOrderStateMachine.java backend/src/test/
  git commit -m "feat(maintenance): implement authoritative SLA calculation and state machine guards"
  ```

---

### Task 5: Application Service, DTOs & Business Operations

**Files:**
- Create DTOs:
  - `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/dto/CreateWorkOrderRequest.java`
  - `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/dto/AssignTechnicianRequest.java`
  - `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/dto/TransitionStatusRequest.java`
  - `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/dto/AddCostLineRequest.java`
  - `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/dto/SubmitFeedbackRequest.java`
  - `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/dto/WorkOrderDto.java`
  - `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/dto/WorkOrderSummaryDto.java`
  - `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/dto/WorkOrderActivityDto.java`
  - `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/dto/WorkOrderCostLineDto.java`
- Create Service:
  - `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/service/WorkOrderService.java`
- Test:
  - `d:/HMSREFERENCE/scos/backend/src/test/java/com/scos/modules/maintenance/service/WorkOrderServiceTest.java`

**Interfaces:**
- Produces:
  - `WorkOrderService.createWorkOrder(CreateWorkOrderRequest req, String username): WorkOrderDto`
  - `WorkOrderService.assignTechnician(UUID workOrderId, AssignTechnicianRequest req, String username): WorkOrderDto`
  - `WorkOrderService.transitionStatus(UUID workOrderId, TransitionStatusRequest req, String username): WorkOrderDto`
  - `WorkOrderService.addCostLine(UUID workOrderId, AddCostLineRequest req, String username): WorkOrderCostLineDto`
  - `WorkOrderService.submitFeedback(UUID workOrderId, SubmitFeedbackRequest req, String username): WorkOrderDto`
  - `WorkOrderService.getSummary(UUID communityId): WorkOrderSummaryDto`
  - `WorkOrderService.getWorkOrders(UUID communityId, WorkOrderStatus status, WorkOrderPriority priority, int page, int size): Page<WorkOrderDto>`

- [ ] **Step 1: Write integration test `WorkOrderServiceTest`**
  - Verify complete lifecycle from `createWorkOrder` -> `assignTechnician` -> `startWork` -> `addCostLine` -> `resolve` -> `submitFeedback`.
  - Verify optimistic locking rejection when version mismatch occurs.
  - Verify private unit without `unitId` throws validation error.

- [ ] **Step 2: Implement `WorkOrderService`**
  - Implement ticket creation, sequence number assignment, automated SLA due timestamp calculation, append-only activity recording, cost sum recalculation, and resident feedback verification.

- [ ] **Step 3: Run Tests**
  Run: `mvn test -Dtest=WorkOrderServiceTest`
  Expected: PASS

- [ ] **Step 4: Commit**
  ```bash
  git add backend/src/main/java/com/scos/modules/maintenance/dto/ backend/src/main/java/com/scos/modules/maintenance/service/WorkOrderService.java backend/src/test/
  git commit -m "feat(maintenance): implement WorkOrderService business logic and DTO mappings"
  ```

---

### Task 6: REST Controller & Security Integration

**Files:**
- Create: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/modules/maintenance/controller/WorkOrderController.java`
- Test: `d:/HMSREFERENCE/scos/backend/src/test/java/com/scos/modules/maintenance/controller/WorkOrderControllerTest.java`

**Endpoints:**
- `POST /api/v1/work-orders` (`@PreAuthorize("hasAuthority('SCREEN:WORK_ORDERS')")`)
- `GET /api/v1/work-orders` (`@PreAuthorize("hasAuthority('SCREEN:WORK_ORDERS')")`)
- `GET /api/v1/work-orders/{id}` (`@PreAuthorize("hasAuthority('SCREEN:WORK_ORDERS')")`)
- `GET /api/v1/work-orders/summary` (`@PreAuthorize("hasAuthority('SCREEN:WORK_ORDERS')")`)
- `PATCH /api/v1/work-orders/{id}/assign` (`@PreAuthorize("hasAuthority('SCREEN:WORK_ORDERS')")`)
- `PATCH /api/v1/work-orders/{id}/status` (`@PreAuthorize("hasAuthority('SCREEN:WORK_ORDERS')")`)
- `POST /api/v1/work-orders/{id}/costs` (`@PreAuthorize("hasAuthority('SCREEN:WORK_ORDERS')")`)
- `POST /api/v1/work-orders/{id}/feedback` (`@PreAuthorize("hasAuthority('SCREEN:WORK_ORDERS')")`)

- [ ] **Step 1: Write Controller MockMvc Test**
  - Test authorized access with `SUPER_ADMIN` token returns `200 OK`.
  - Test unauthorized user without `SCREEN:WORK_ORDERS` returns `403 Forbidden`.
  - Test status transition and cost addition.

- [ ] **Step 2: Implement `WorkOrderController`**
  - Wire all endpoints, bind `@AuthenticationPrincipal` for current username/tenant, return standardized `ApiResponse<T>`.

- [ ] **Step 3: Run Tests**
  Run: `mvn test -Dtest=WorkOrderControllerTest`
  Expected: PASS

- [ ] **Step 4: Commit**
  ```bash
  git add backend/src/main/java/com/scos/modules/maintenance/controller/ backend/src/test/
  git commit -m "feat(maintenance): add WorkOrderController REST endpoints with RBAC guards"
  ```

---

### Task 7: DataInitializer Seed Data for Live Testing

**Files:**
- Modify: `d:/HMSREFERENCE/scos/backend/src/main/java/com/scos/config/DataInitializer.java`

- [ ] **Step 1: Update `DataInitializer.java`**
  - Add seeding for sample work orders in various operational states (`OPEN`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`) with initial cost lines and activity timelines.
  - Create a test technician user `technician.ramesh` (`Password@123`, role `TECHNICIAN`) for assignment testing.

- [ ] **Step 2: Run Compilation and Startup Verification**
  Run: `mvn test-compile -DskipTests`
  Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**
  ```bash
  git add backend/src/main/java/com/scos/config/DataInitializer.java
  git commit -m "feat(maintenance): seed operational work orders and technician user in DataInitializer"
  ```

---

### Task 8: Angular Frontend Services & Models

**Files:**
- Create: `d:/HMSREFERENCE/scos/frontend/src/app/features/work-orders/work-order.model.ts`
- Create: `d:/HMSREFERENCE/scos/frontend/src/app/features/work-orders/work-order.service.ts`

- [ ] **Step 1: Create TypeScript Models (`work-order.model.ts`)**
  - Define `WorkOrder`, `WorkOrderSummary`, `WorkOrderActivity`, `WorkOrderCostLine`, `CreateWorkOrderRequest`, `AssignTechnicianRequest`, `TransitionStatusRequest`, `AddCostLineRequest`, `SubmitFeedbackRequest`.

- [ ] **Step 2: Create `WorkOrderService`**
  - Methods: `getWorkOrders`, `getSummary`, `getWorkOrderById`, `createWorkOrder`, `assignTechnician`, `transitionStatus`, `addCostLine`, `submitFeedback`.

- [ ] **Step 3: Test compilation**
  Run: `npx ng build --configuration=development`
  Expected: `Application bundle generation complete`

- [ ] **Step 4: Commit**
  ```bash
  git add frontend/src/app/features/work-orders/
  git commit -m "feat(work-orders): add TypeScript interfaces and WorkOrderService REST client"
  ```

---

### Task 9: Angular Frontend Operations Console (Kanban + Table + Workbench)

**Files:**
- Create: `d:/HMSREFERENCE/scos/frontend/src/app/features/work-orders/work-order-list.component.ts`
- Create: `d:/HMSREFERENCE/scos/frontend/src/app/features/work-orders/work-order-workbench-drawer.component.ts`
- Create: `d:/HMSREFERENCE/scos/frontend/src/app/features/work-orders/create-work-order-modal.component.ts`

- [ ] **Step 1: Implement `WorkOrderListComponent`**
  - KPI Tiles: Active Work Orders, Open Queue, In Progress, SLA Overdue, Resident Satisfaction Rating.
  - View switcher: Toggle between **Kanban Board** and **Data Table**.
  - **Kanban Board**: 4 columns (`OPEN`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`) with SLA countdown badges, priority chips, unit tags, and quick-move buttons.
  - **Data Table**: Filterable, sortable list with status indicator dots, priority tags, and action buttons.
  - Click on any card/row opens the slide-over workbench drawer.

- [ ] **Step 2: Implement `WorkOrderWorkbenchDrawerComponent`**
  - Slide-over panel preserving list context.
  - Status transition action bar displaying only allowed actions for current ticket state.
  - Technician assignment picker with quick dispatch.
  - Tabbed sections:
    1. **Overview & Details**: Location, reporter, description, SLA remaining countdown.
    2. **Itemized Costs**: Labor, parts, materials ledger with add-cost form.
    3. **Activity Timeline**: Append-only chronological audit log.
    4. **Resident Sign-off & Feedback**: 5-star rating widget with comments.

- [ ] **Step 3: Implement `CreateWorkOrderModalComponent`**
  - Accessible dialog for filing issues.
  - Toggle between `PRIVATE_UNIT` (shows building & unit pickers) and `COMMON_AREA` (shows building and location details).
  - Category and priority selectors with SLA target indication (e.g. "Critical: 4-hour emergency response").

- [ ] **Step 4: Commit**
  ```bash
  git add frontend/src/app/features/work-orders/
  git commit -m "feat(work-orders): implement Kanban board, data table, and slide-over workbench drawer"
  ```

---

### Task 10: Routing, Sidebar & Security Guards Integration

**Files:**
- Modify: `d:/HMSREFERENCE/scos/frontend/src/app/core/layout/sidebar.component.ts`
- Modify: `d:/HMSREFERENCE/scos/frontend/src/app/app.routes.ts`

- [ ] **Step 1: Update `sidebar.component.ts`**
  - Add `Work Orders & Maintenance Desk` navigation item under `CORE OPERATIONS` with icon `🛠️`, guarded by `authService.hasPermission('SCREEN:WORK_ORDERS')`.

- [ ] **Step 2: Update `app.routes.ts`**
  - Add route `/work-orders` with `canActivate: [authGuard]` and `data: { permission: 'SCREEN:WORK_ORDERS' }`.

- [ ] **Step 3: Test Frontend Build**
  Run: `npm run build` (inside `scos/frontend`)
  Expected: `Application bundle generation complete` with 0 errors.

- [ ] **Step 4: Commit**
  ```bash
  git add frontend/src/app/core/layout/sidebar.component.ts frontend/src/app/app.routes.ts
  git commit -m "feat(work-orders): connect work orders route and sidebar navigation with permission guards"
  ```

---

### Task 11: End-to-End Build & Browser Verification

**Files:**
- Test execution across both backend and frontend.

- [ ] **Step 1: Execute Full Backend Test Suite**
  Run: `mvn test` (inside `scos/backend`)
  Expected: All tests pass with 0 failures.

- [ ] **Step 2: Restart Spring Boot Backend with V5 Migration**
  Run: `mvn spring-boot:run`
  Expected: Flyway applies `V5__init_work_orders_and_maintenance_desk.sql`, `DataInitializer` seeds test tickets, Tomcat starts on port 8080.

- [ ] **Step 3: Browser Subagent Verification Session**
  - Open `http://localhost:4200/login`, authenticate as `admin`.
  - Navigate to `Work Orders Desk` from sidebar.
  - Verify KPI metrics (Active tickets, In Progress, SLA Overdue, Avg Rating).
  - Verify Kanban Board columns.
  - Click "+ File Work Order" modal, create a high-priority plumbing ticket in Unit `A-1201`.
  - Confirm ticket appears in `OPEN` column with SLA badge.
  - Open ticket in slide-over Workbench Drawer:
    - Assign to technician.
    - Transition state to `IN_PROGRESS`.
    - Record a material cost line item (`Replacement Ball Valve`, ₹450).
    - Mark `RESOLVED`.
    - Submit 5-star resident feedback verification.
    - Confirm ticket reaches `CLOSED`.
  - Capture recording and screenshot artifacts.

- [ ] **Step 4: Commit final verification notes**
  ```bash
  git add .
  git commit -m "chore(maintenance): complete end-to-end verification of Work Orders Desk"
  ```
