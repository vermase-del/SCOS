-- V5: SCOS Work Orders & Maintenance Desk Schema
-- Tenant-scoped work orders with optimistic locking, SLA tracking, history and cost ledger.

CREATE TABLE scos_work_order_sequences (
    community_id UUID NOT NULL REFERENCES scos_communities(id) ON DELETE CASCADE,
    year INT NOT NULL CHECK (year BETWEEN 2000 AND 9999),
    current_sequence BIGINT NOT NULL DEFAULT 0 CHECK (current_sequence >= 0),
    PRIMARY KEY (community_id, year)
);

CREATE TABLE scos_work_orders (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES scos_communities(id) ON DELETE CASCADE,
    ticket_number VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    scope VARCHAR(20) NOT NULL CHECK (scope IN ('PRIVATE_UNIT', 'COMMON_AREA')),
    building_id UUID REFERENCES scos_buildings(id) ON DELETE SET NULL,
    unit_id UUID REFERENCES scos_units(id) ON DELETE SET NULL,
    location_details VARCHAR(200),
    category VARCHAR(40) NOT NULL CHECK (category IN ('PLUMBING', 'ELECTRICAL', 'HVAC', 'ELEVATOR', 'CIVIL', 'SECURITY_DEVICE', 'GARDENING', 'OTHER')),
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'RESOLVED', 'CLOSED', 'CANCELLED')),
    reported_by_user_id UUID NOT NULL REFERENCES scos_users(id),
    assigned_technician_id UUID REFERENCES scos_users(id),
    sla_policy_version VARCHAR(20) NOT NULL DEFAULT 'v1',
    sla_due_at TIMESTAMPTZ NOT NULL,
    sla_breached BOOLEAN NOT NULL DEFAULT FALSE,
    hold_reason VARCHAR(60),
    hold_started_at TIMESTAMPTZ,
    accumulated_hold_seconds BIGINT NOT NULL DEFAULT 0 CHECK (accumulated_hold_seconds >= 0),
    total_cost NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (total_cost >= 0),
    currency_code VARCHAR(3) NOT NULL DEFAULT 'INR',
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(60) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(60) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wo_community_ticket UNIQUE (community_id, ticket_number),
    CONSTRAINT ck_wo_private_unit CHECK (scope <> 'PRIVATE_UNIT' OR unit_id IS NOT NULL),
    CONSTRAINT ck_wo_hold_fields CHECK (
      (status = 'ON_HOLD' AND hold_started_at IS NOT NULL AND hold_reason IS NOT NULL)
      OR (status <> 'ON_HOLD')
    )
);

CREATE INDEX idx_wo_community_status ON scos_work_orders(community_id, status);
CREATE INDEX idx_wo_sla_due ON scos_work_orders(community_id, sla_due_at, sla_breached);
CREATE INDEX idx_wo_technician ON scos_work_orders(community_id, assigned_technician_id, status);
CREATE INDEX idx_wo_created_at ON scos_work_orders(community_id, created_at DESC);

CREATE TABLE scos_work_order_activities (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL REFERENCES scos_work_orders(id) ON DELETE CASCADE,
    author_user_id UUID NOT NULL REFERENCES scos_users(id),
    activity_type VARCHAR(40) NOT NULL CHECK (activity_type IN ('STATUS_TRANSITION', 'ASSIGNMENT', 'NOTE_ADDED', 'HOLD_PLACED', 'HOLD_RESUMED', 'COST_RECORDED', 'VERIFIED_CLOSED', 'CREATED', 'SLA_BREACHED')),
    from_status VARCHAR(30),
    to_status VARCHAR(30),
    notes TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_wo_activities_ticket ON scos_work_order_activities(work_order_id, created_at);

CREATE TABLE scos_work_order_assignments (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL REFERENCES scos_work_orders(id) ON DELETE CASCADE,
    technician_id UUID NOT NULL REFERENCES scos_users(id),
    assigned_by_user_id UUID NOT NULL REFERENCES scos_users(id),
    notes VARCHAR(255),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_wo_assignments_ticket ON scos_work_order_assignments(work_order_id, assigned_at);

CREATE TABLE scos_work_order_cost_lines (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL REFERENCES scos_work_orders(id) ON DELETE CASCADE,
    cost_type VARCHAR(30) NOT NULL CHECK (cost_type IN ('LABOR', 'MATERIALS', 'VENDOR_FEE', 'EQUIPMENT', 'OTHER')),
    item_description VARCHAR(160) NOT NULL,
    quantity NUMERIC(10,2) NOT NULL DEFAULT 1.00 CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    total_amount NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    currency_code VARCHAR(3) NOT NULL DEFAULT 'INR',
    recorded_by_user_id UUID NOT NULL REFERENCES scos_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_wo_cost_lines_ticket ON scos_work_order_cost_lines(work_order_id, created_at);

CREATE TABLE scos_work_order_feedback (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL UNIQUE REFERENCES scos_work_orders(id) ON DELETE CASCADE,
    resident_user_id UUID NOT NULL REFERENCES scos_users(id),
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    feedback_comments TEXT,
    resolution_confirmed BOOLEAN NOT NULL,
    is_auto_closed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE scos_work_order_sla_history (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL REFERENCES scos_work_orders(id) ON DELETE CASCADE,
    event_type VARCHAR(40) NOT NULL CHECK (event_type IN ('HOLD', 'RESUME', 'DEADLINE_ADJUSTED', 'BREACH')),
    original_due_at TIMESTAMPTZ NOT NULL,
    new_due_at TIMESTAMPTZ NOT NULL,
    hold_duration_seconds BIGINT CHECK (hold_duration_seconds IS NULL OR hold_duration_seconds >= 0),
    reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_wo_sla_history_ticket ON scos_work_order_sla_history(work_order_id, created_at);

-- Enforce append-only semantics for the operational timeline.
CREATE OR REPLACE FUNCTION scos_reject_work_order_activity_mutation()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Work order activities are append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_wo_activity_no_update
BEFORE UPDATE OR DELETE ON scos_work_order_activities
FOR EACH ROW EXECUTE FUNCTION scos_reject_work_order_activity_mutation();
