-- =========================================================================
-- V4: SCOS Screen-Level Permissions & Dynamic RBAC Matrix
-- =========================================================================

-- Insert Screen Permissions
INSERT INTO scos_permissions (id, code, module, description) VALUES
    ('00000000-0000-0000-0001-000000000001', 'SCREEN:DASHBOARD', 'SCREEN', 'Access Executive Operations Dashboard'),
    ('00000000-0000-0000-0001-000000000002', 'SCREEN:BUILDING_MATRIX', 'SCREEN', 'Access Interactive 2D Building Floor Matrix'),
    ('00000000-0000-0000-0001-000000000003', 'SCREEN:USER_DIRECTORY', 'SCREEN', 'Access Resident & Staff User Directory'),
    ('00000000-0000-0000-0001-000000000004', 'SCREEN:USER_CREATE', 'SCREEN', 'Invite and provision new user accounts'),
    ('00000000-0000-0000-0001-000000000005', 'SCREEN:ROLE_MANAGEMENT', 'SCREEN', 'Configure Role-Permission Access Matrix'),
    ('00000000-0000-0000-0001-000000000006', 'SCREEN:COMMUNITY_REGISTER', 'SCREEN', 'Register and onboard new housing societies'),
    ('00000000-0000-0000-0001-000000000007', 'SCREEN:GATE_SECURITY', 'SCREEN', 'Gatehouse Security & Visitor Terminal'),
    ('00000000-0000-0000-0001-000000000008', 'SCREEN:WORK_ORDERS', 'SCREEN', 'Maintenance & Facility Work Order Desk')
ON CONFLICT (code) DO NOTHING;

-- Grant All Permissions to SUPER_ADMIN
INSERT INTO scos_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM scos_roles r CROSS JOIN scos_permissions p
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- Grant Permissions to COMMUNITY_MANAGER
INSERT INTO scos_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM scos_roles r CROSS JOIN scos_permissions p
WHERE r.code = 'COMMUNITY_MANAGER'
  AND p.code IN (
    'SCREEN:DASHBOARD', 'SCREEN:BUILDING_MATRIX', 'SCREEN:USER_DIRECTORY',
    'SCREEN:USER_CREATE', 'SCREEN:ROLE_MANAGEMENT', 'SCREEN:COMMUNITY_REGISTER',
    'SCREEN:GATE_SECURITY', 'SCREEN:WORK_ORDERS',
    'COMMUNITY:READ', 'COMMUNITY:WRITE',
    'BUILDING:READ', 'BUILDING:WRITE',
    'UNIT:READ', 'UNIT:WRITE',
    'USER:READ', 'USER:WRITE',
    'WORKORDER:READ', 'WORKORDER:WRITE',
    'GATE:CHECKIN'
  )
ON CONFLICT DO NOTHING;

-- Grant Permissions to HOA_OFFICER
INSERT INTO scos_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM scos_roles r CROSS JOIN scos_permissions p
WHERE r.code = 'HOA_OFFICER'
  AND p.code IN (
    'SCREEN:DASHBOARD', 'SCREEN:BUILDING_MATRIX', 'SCREEN:USER_DIRECTORY', 'SCREEN:WORK_ORDERS',
    'COMMUNITY:READ', 'BUILDING:READ', 'UNIT:READ', 'USER:READ', 'WORKORDER:READ'
  )
ON CONFLICT DO NOTHING;

-- Grant Permissions to OWNER
INSERT INTO scos_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM scos_roles r CROSS JOIN scos_permissions p
WHERE r.code = 'OWNER'
  AND p.code IN (
    'SCREEN:DASHBOARD', 'SCREEN:BUILDING_MATRIX', 'SCREEN:WORK_ORDERS',
    'BUILDING:READ', 'UNIT:READ', 'WORKORDER:READ', 'WORKORDER:WRITE'
  )
ON CONFLICT DO NOTHING;

-- Grant Permissions to TENANT
INSERT INTO scos_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM scos_roles r CROSS JOIN scos_permissions p
WHERE r.code = 'TENANT'
  AND p.code IN (
    'SCREEN:DASHBOARD', 'SCREEN:BUILDING_MATRIX', 'SCREEN:WORK_ORDERS',
    'BUILDING:READ', 'UNIT:READ', 'WORKORDER:READ', 'WORKORDER:WRITE'
  )
ON CONFLICT DO NOTHING;

-- Grant Permissions to SECURITY_GUARD
INSERT INTO scos_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM scos_roles r CROSS JOIN scos_permissions p
WHERE r.code = 'SECURITY_GUARD'
  AND p.code IN (
    'SCREEN:GATE_SECURITY', 'GATE:CHECKIN', 'UNIT:READ'
  )
ON CONFLICT DO NOTHING;

-- Grant Permissions to TECHNICIAN
INSERT INTO scos_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM scos_roles r CROSS JOIN scos_permissions p
WHERE r.code = 'TECHNICIAN'
  AND p.code IN (
    'SCREEN:WORK_ORDERS', 'WORKORDER:READ', 'WORKORDER:WRITE', 'UNIT:READ'
  )
ON CONFLICT DO NOTHING;
