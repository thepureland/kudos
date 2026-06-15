-- auth_role_exclusion fixtures for AuthRoleExclusionDaoTest.
-- Canonical order (role_a_id < role_b_id) is assumed already satisfied by these ids.
-- tenant1 has pairs (rA,rB) and (rA,rC); tenant2 has (rA,rB) — same ids, different tenant,
-- to prove tenant scoping in searchByRoleIdAndTenant / pairExistsForTenant.
merge into "auth_role_exclusion" ("id", "role_a_id", "role_b_id", "tenant_id", "description", "create_user_id", "create_user_name") values
    ('9f2c0000-0000-0000-0000-0000000000e1', '9f2c0000-0000-0000-0000-0000000000a0', '9f2c0000-0000-0000-0000-0000000000b0', 'excl-tenant-1-9f2c', 'A<->B in tenant1', 'system', '系统'),
    ('9f2c0000-0000-0000-0000-0000000000e2', '9f2c0000-0000-0000-0000-0000000000a0', '9f2c0000-0000-0000-0000-0000000000c0', 'excl-tenant-1-9f2c', 'A<->C in tenant1', 'system', '系统'),
    ('9f2c0000-0000-0000-0000-0000000000e3', '9f2c0000-0000-0000-0000-0000000000a0', '9f2c0000-0000-0000-0000-0000000000b0', 'excl-tenant-2-9f2c', 'A<->B in tenant2', 'system', '系统');
