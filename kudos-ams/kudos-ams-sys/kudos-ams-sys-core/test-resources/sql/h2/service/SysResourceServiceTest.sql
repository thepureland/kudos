merge into "sys_sub_system" ("code", "name", "system_code", "remark", "active", "built_in")
    values ('svc-subsys-res-test-1', 'svc-subsys-res-test-1-name', 'default', 'from SysResourceServiceTest', true, false);

merge into "sys_resource" ("id", "name", "url", "resource_type_dict_code", "parent_id", "order_num", "sub_system_code", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000031', 'svc-res-test-1', '/svc-res-test-1', '1', null, 1, 'svc-subsys-res-test-1', 'from SysResourceServiceTest', true, false),
           ('20000000-0000-0000-0000-000000000032', 'svc-res-test-2', '/svc-res-test-2', '2', '20000000-0000-0000-0000-000000000031', 2, 'svc-subsys-res-test-1', 'from SysResourceServiceTest', true, false);
