-- =========================================================================
-- V2: SCOS Building & Units Infrastructure Schema
-- =========================================================================

CREATE TABLE scos_buildings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES scos_communities(id) ON DELETE CASCADE,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(80) NOT NULL,
    total_floors INT NOT NULL,
    basement_floors INT NOT NULL DEFAULT 1,
    total_units INT NOT NULL DEFAULT 0,
    has_elevator BOOLEAN NOT NULL DEFAULT TRUE,
    handover_date DATE,
    construction_status VARCHAR(25) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    version INT NOT NULL DEFAULT 0,
    UNIQUE (community_id, code)
);

CREATE TABLE scos_units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES scos_communities(id) ON DELETE CASCADE,
    building_id UUID NOT NULL REFERENCES scos_buildings(id) ON DELETE RESTRICT,
    unit_number VARCHAR(20) NOT NULL,
    floor_number INT NOT NULL,
    unit_type VARCHAR(30) NOT NULL,
    carpet_area_sqft NUMERIC(10,2) NOT NULL,
    super_builtup_area_sqft NUMERIC(10,2) NOT NULL,
    balcony_count INT NOT NULL DEFAULT 1,
    bathroom_count INT NOT NULL DEFAULT 2,
    parking_slot_number VARCHAR(50),
    occupancy_status VARCHAR(25) NOT NULL DEFAULT 'VACANT',
    furnishing_status VARCHAR(20) NOT NULL DEFAULT 'SEMI_FURNISHED',
    electric_meter_number VARCHAR(50),
    gas_meter_number VARCHAR(50),
    water_meter_number VARCHAR(50),
    base_maintenance_charge NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    version INT NOT NULL DEFAULT 0,
    UNIQUE (building_id, unit_number)
);

CREATE INDEX idx_units_building_floor ON scos_units(building_id, floor_number);
CREATE INDEX idx_units_occupancy ON scos_units(community_id, occupancy_status);

CREATE TABLE scos_unit_occupants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID NOT NULL REFERENCES scos_units(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES scos_users(id) ON DELETE CASCADE,
    occupancy_type VARCHAR(25) NOT NULL,
    is_primary_contact BOOLEAN NOT NULL DEFAULT FALSE,
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (unit_id, user_id, occupancy_type)
);
