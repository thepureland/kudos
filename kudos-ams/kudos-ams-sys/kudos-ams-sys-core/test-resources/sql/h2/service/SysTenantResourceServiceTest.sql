merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000035', 'svc-tenant-tr-test-1', null, null, 'from SysTenantResourceServiceTest', true, false);

merge into "sys_sub_system" ("code", "name", "portal_code", "remark", "active", "built_in")
    values ('svc-subsys-tr-test-1', 'svc-subsys-tr-test-1-name', 'default', 'from SysTenantResourceServiceTest', true, false);

merge into "sys_resource" ("id", "name", "url", "resource_type_dict_code", "parent_id", "order_num", "sub_system_code", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000035', 'svc-res-tr-test-1', '/svc-res-tr-test-1', '1', null, 1, 'svc-subsys-tr-test-1', 'from SysTenantResourceServiceTest', true, false);

merge into "sys_tenant_resource" ("id", "tenant_id", "resource_id")
    values ('20000000-0000-0000-0000-000000000035', '20000000-0000-0000-0000-000000000035', '20000000-0000-0000-0000-000000000035');
