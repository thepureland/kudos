merge into "user_org" (
    "id", "name", "tenant_id", "org_type_dict_code", "sort_num", "active", "built_in"
) key ("id") values (
    '10000000-0000-0000-0000-000000000001', 'JIT Default Organization',
    'jit-provisioning-tenant', 'DEFAULT', 0, true, false
);
