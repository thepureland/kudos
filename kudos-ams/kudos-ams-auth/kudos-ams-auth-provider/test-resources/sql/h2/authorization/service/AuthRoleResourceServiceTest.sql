-- 测试数据：AuthRoleResourceServiceTest
-- 使用唯一前缀 svc-roleres-test-* 和唯一UUID确保测试数据隔离

-- 创建测试用的用户（用于测试资源缓存同步）
merge into "auth_user" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('3248fb0d-0000-0000-0000-000000000055', 'svc-user-roleres-test-1-fHos3r3I', 'svc-tenan-roler-test-1-fHos3r3I', 'encrypted-pwd-1-fHos3r3I', '00000000-0000-0000-0000-000000000000', 'from AuthRoleResourceServiceTest', true, false, 'system', '系统');

-- 创建测试用的角色
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('3248fb0d-0000-0000-0000-000000000050', 'svc-role-roleres-test-1-fHos3r3I', 'svc-rol-rol-tes-1-name-fHos3r3I', 'svc-tenan-roler-test-1-fHos3r3I', 'ams', 'from AuthRoleResourceServiceTest', true, false, 'system', '系统'),
           ('3248fb0d-0000-0000-0000-000000000051', 'svc-role-roleres-test-2-fHos3r3I', 'svc-rol-rol-tes-2-name-fHos3r3I', 'svc-tenan-roler-test-1-fHos3r3I', 'ams', 'from AuthRoleResourceServiceTest', true, false, 'system', '系统');

-- 创建测试用的资源（使用独立的资源ID，假设这些资源ID在sys_resource表中存在）
-- 注意：实际测试中需要确保这些资源ID在sys_resource表中存在，或者使用真实的资源ID
-- 这里使用独立的资源ID前缀 30000000-0000-0000-0000-0000000000XX 确保不与其他测试冲突

-- 创建已存在的角色-资源关系（用于测试exists和unbind）
merge into "auth_role_resource" ("id", "role_id", "resource_id", "create_user_id", "create_user_name")
    values ('3248fb0d-0000-0000-0000-000000000052', '3248fb0d-0000-0000-0000-000000000050', '3248fb0d-0000-0000-0000-000000000056', 'system', '系统'),
           ('3248fb0d-0000-0000-0000-000000000053', '3248fb0d-0000-0000-0000-000000000050', '3248fb0d-0000-0000-0000-000000000057', 'system', '系统');

-- 创建角色-用户关系（用于测试资源缓存同步）
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name")
    values ('3248fb0d-0000-0000-0000-000000000054', '3248fb0d-0000-0000-0000-000000000050', '3248fb0d-0000-0000-0000-000000000055', 'system', '系统');
