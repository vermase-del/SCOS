-- V6: SCOS asset register and richer maintenance work-order planning.
-- Derived from useful maintenance/room-status patterns in the supplied DBSchema.json.
-- V5 is left immutable; this additive migration preserves Flyway history.

CREATE TABLE scos_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES scos_communities(id) ON DELETE CASCADE,
    asset_code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    asset_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPERATIONAL'
        CHECK (status IN ('OPERATIONAL', 'UNDER_MAINTENANCE', 'OUT_OF_SERVICE', 'RETIRED')),
    criticality VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'
        CHECK (criticality IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    building_id UUID REFERENCES scos_buildings(id) ON DELETE RESTRICT,
    unit_id UUID REFERENCES scos_units(id) ON DELETE RESTRICT,
    location_details VARCHAR(200),
    manufacturer VARCHAR(100),
    model_number VARCHAR(100),
    serial_number VARCHAR(120),
    installed_on DATE,
    warranty_expires_on DATE,
    last_maintenance_at TIMESTAMPTZ,
    next_maintenance_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(60) NOT NULL DEFAULT 'system',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(60) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_scos_asset_community_code UNIQUE (community_id, asset_code),
    CONSTRAINT ck_scos_asset_location CHECK (building_id IS NOT NULL OR unit_id IS NULL)
);

CREATE INDEX idx_scos_assets_community_type_status
    ON scos_assets (community_id, asset_type, status);
CREATE INDEX idx_scos_assets_building
    ON scos_assets (community_id, building_id) WHERE building_id IS NOT NULL;
CREATE INDEX idx_scos_assets_unit
    ON scos_assets (community_id, unit_id) WHERE unit_id IS NOT NULL;
CREATE INDEX idx_scos_assets_next_maintenance
    ON scos_assets (community_id, next_maintenance_at) WHERE is_active = TRUE;

CREATE TABLE scos_asset_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES scos_communities(id) ON DELETE CASCADE,
    asset_id UUID NOT NULL REFERENCES scos_assets(id) ON DELETE RESTRICT,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL
        CHECK (to_status IN ('OPERATIONAL', 'UNDER_MAINTENANCE', 'OUT_OF_SERVICE', 'RETIRED')),
    reason TEXT,
    changed_by_user_id UUID REFERENCES scos_users(id) ON DELETE SET NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_scos_asset_status_history_asset
    ON scos_asset_status_history (community_id, asset_id, changed_at DESC);

ALTER TABLE scos_work_orders
    ADD COLUMN work_order_type VARCHAR(30) NOT NULL DEFAULT 'REACTIVE'
        CHECK (work_order_type IN ('REACTIVE', 'PREVENTIVE', 'INSPECTION', 'EMERGENCY', 'CORRECTIVE')),
    ADD COLUMN reason_code VARCHAR(40),
    ADD COLUMN department_code VARCHAR(50),
    ADD COLUMN planned_start_at TIMESTAMPTZ,
    ADD COLUMN planned_end_at TIMESTAMPTZ,
    ADD COLUMN actual_start_at TIMESTAMPTZ,
    ADD COLUMN resolved_at TIMESTAMPTZ,
    ADD COLUMN out_of_service BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN service_type VARCHAR(40),
    ADD COLUMN asset_id UUID REFERENCES scos_assets(id) ON DELETE RESTRICT,
    ADD COLUMN external_reference VARCHAR(120),
    ADD CONSTRAINT ck_wo_planned_window
        CHECK (planned_start_at IS NULL OR planned_end_at IS NULL OR planned_end_at >= planned_start_at);

CREATE INDEX idx_wo_asset_status
    ON scos_work_orders (community_id, asset_id, status) WHERE asset_id IS NOT NULL;
CREATE INDEX idx_wo_planned_window
    ON scos_work_orders (community_id, planned_start_at, planned_end_at)
    WHERE planned_start_at IS NOT NULL;
CREATE INDEX idx_wo_type_status
    ON scos_work_orders (community_id, work_order_type, status);

-- A status transition is recorded as an append-only event, while the current
-- asset status is kept on scos_assets for efficient operational queries.
CREATE OR REPLACE FUNCTION scos_reject_asset_status_history_mutation()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Asset status history is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_scos_asset_status_history_no_update
BEFORE UPDATE OR DELETE ON scos_asset_status_history
FOR EACH ROW EXECUTE FUNCTION scos_reject_asset_status_history_mutation();
