-- SoD 并发竞态用例:一个用户 + 一对互斥角色。
-- 两个线程同时把 A 和 B 授给同一个人,看"先查后写"之间的窗口是否会让两条都落库。

merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('50d00000-0000-0000-0000-0000000000e1', 'sod-race-user-50d0', 'sod-race-tenant-50d0', 'pwd', '00000000-0000-0000-0000-000000000000', 'from SoDConcurrencyTest', true, false, 'system', '系统');

-- 两个角色本身毫无问题,问题只在"同一人不得兼任"
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('50d00000-0000-0000-0000-0000000000a1', 'sod-race-maker-50d0', '制单员', 'sod-race-tenant-50d0', 'ams', 'from SoDConcurrencyTest', true, false, 'system', '系统'),
    ('50d00000-0000-0000-0000-0000000000a2', 'sod-race-checker-50d0', '审核员', 'sod-race-tenant-50d0', 'ams', 'from SoDConcurrencyTest', true, false, 'system', '系统');

-- 经典的不相容职责:制单与审核不得同人
merge into "auth_role_exclusion" ("id", "role_a_id", "role_b_id", "tenant_id", "description", "create_user_id", "create_user_name") values
    ('50d00000-0000-0000-0000-0000000000f1', '50d00000-0000-0000-0000-0000000000a1', '50d00000-0000-0000-0000-0000000000a2', 'sod-race-tenant-50d0', '制单与审核不得同人', 'system', '系统');
