-- =========================================================================
-- V3: Seed Initial Roles, Permissions & Sample Community
-- =========================================================================

-- Roles
INSERT INTO scos_roles (id, code, name, description, is_system_role) VALUES
    ('00000000-0000-0000-0000-000000000001', 'SUPER_ADMIN', 'Super Administrator', 'System-wide administrator', TRUE),
    ('00000000-0000-0000-0000-000000000002', 'COMMUNITY_MANAGER', 'Community Manager', 'Estate administrator for residential complex', TRUE),
    ('00000000-0000-0000-0000-000000000003', 'HOA_OFFICER', 'HOA / Society Board Member', 'Elected executive board member', TRUE),
    ('00000000-0000-0000-0000-000000000004', 'OWNER', 'Apartment Owner', 'Registered property owner / landlord', TRUE),
    ('00000000-0000-0000-0000-000000000005', 'TENANT', 'Resident Tenant', 'Primary apartment leaseholder', TRUE),
    ('00000000-0000-0000-0000-000000000006', 'SECURITY_GUARD', 'Security Guard', 'Gatehouse security terminal operator', TRUE),
    ('00000000-0000-0000-0000-000000000007', 'TECHNICIAN', 'Maintenance Technician', 'Facility maintenance personnel', TRUE);

-- Permissions
INSERT INTO scos_permissions (code, module, description) VALUES
    ('COMMUNITY:READ', 'COMMUNITY', 'View community details'),
    ('COMMUNITY:WRITE', 'COMMUNITY', 'Modify community configurations'),
    ('BUILDING:READ', 'INFRASTRUCTURE', 'View buildings and floor plans'),
    ('BUILDING:WRITE', 'INFRASTRUCTURE', 'Create and modify buildings/towers'),
    ('UNIT:READ', 'INFRASTRUCTURE', 'View apartment units and occupancy'),
    ('UNIT:WRITE', 'INFRASTRUCTURE', 'Create and modify units and status'),
    ('USER:READ', 'USER', 'View resident and staff user directory'),
    ('USER:WRITE', 'USER', 'Create and manage users and role assignments'),
    ('WORKORDER:READ', 'MAINTENANCE', 'View maintenance work orders'),
    ('WORKORDER:WRITE', 'MAINTENANCE', 'Create and assign maintenance tickets'),
    ('GATE:CHECKIN', 'GATE', 'Process visitor check-ins at security gate');

-- Super Admin User (Password: Password@123 -> BCrypt: $2a$10$7Z2v7f4tQW8qGk5H4/mJ..h3z2X/7f0LpQ3j7K2mH8w1xL9k8J0Oe)
INSERT INTO scos_users (id, username, email, phone_number, password_hash, first_name, last_name, is_active, is_verified) VALUES
    ('11111111-1111-1111-1111-111111111111', 'admin', 'admin@scos.local', '+919999900001', '$2a$10$wumkUHhHsZHm/mG2BeokjeNeyTHV5YJ0WRHgFh7hlHVtbbqzEUFJ2', 'System', 'Admin', TRUE, TRUE);

-- Sample Community: Emerald Grand Society
INSERT INTO scos_communities (id, code, name, legal_name, address_line1, city, state_province, postal_code, country_code, phone, email) VALUES
    ('22222222-2222-2222-2222-222222222222', 'EMERALD_GRAND', 'Emerald Grand Heights', 'Emerald Grand Residents Welfare Association', '100 Outer Ring Road', 'Bangalore', 'Karnataka', '560103', 'IND', '+918022334455', 'manager@emeraldgrand.org');

-- Associate Admin with Community
INSERT INTO scos_user_community_access (user_id, community_id, role_id, status) VALUES
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', '00000000-0000-0000-0000-000000000001', 'ACTIVE');

-- Sample Tower: Tower A
INSERT INTO scos_buildings (id, community_id, code, name, total_floors, total_units, has_elevator, construction_status) VALUES
    ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'TOWER_A', 'Tower A - Emerald Royal', 12, 48, TRUE, 'COMPLETED');

-- Sample Units for Tower A (Floors 10, 11, 12)
INSERT INTO scos_units (id, community_id, building_id, unit_number, floor_number, unit_type, carpet_area_sqft, super_builtup_area_sqft, occupancy_status, base_maintenance_charge) VALUES
    (gen_random_uuid(), '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'A-1201', 12, '3BHK', 1550.00, 1920.00, 'OWNER_OCCUPIED', 4800.00),
    (gen_random_uuid(), '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'A-1202', 12, '3BHK', 1550.00, 1920.00, 'TENANT_OCCUPIED', 4800.00),
    (gen_random_uuid(), '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'A-1203', 12, '2BHK', 1150.00, 1420.00, 'VACANT', 3550.00),
    (gen_random_uuid(), '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'A-1204', 12, '2BHK', 1150.00, 1420.00, 'UNDER_MAINTENANCE', 3550.00),
    (gen_random_uuid(), '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'A-1101', 11, '3BHK', 1550.00, 1920.00, 'OWNER_OCCUPIED', 4800.00),
    (gen_random_uuid(), '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'A-1102', 11, '3BHK', 1550.00, 1920.00, 'NOTICE_PERIOD', 4800.00),
    (gen_random_uuid(), '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'A-1103', 11, '2BHK', 1150.00, 1420.00, 'TENANT_OCCUPIED', 3550.00),
    (gen_random_uuid(), '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'A-1104', 11, '2BHK', 1150.00, 1420.00, 'VACANT', 3550.00);
