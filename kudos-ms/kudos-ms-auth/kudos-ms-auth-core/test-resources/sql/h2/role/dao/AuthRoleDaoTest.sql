-- auth_role: 每条 id 唯一，使用 auth-role-dao-test-* 前缀隔离
-- 基础三角色：t1 下两个 active，t2 下一个 inactive
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('7e4a2d1b-0000-0000-0000-000000000020', 'auth-role-dao-test-1-DqEGww0j', 'auth-rol-dao-tes-1-name-DqEGww0j', 'auth-tenant-dao-test-1-DqEGww0j', 'ams', 'from AuthRoleDaoTest', true, false, 'system', '系统'),
    ('7e4a2d1b-0000-0000-0000-000000000021', 'auth-role-dao-test-2-DqEGww0j', 'auth-rol-dao-tes-2-name-DqEGww0j', 'auth-tenant-dao-test-1-DqEGww0j', 'ams', 'from AuthRoleDaoTest', true, false, 'system', '系统'),
    ('7e4a2d1b-0000-0000-0000-000000000022', 'auth-role-dao-test-3-DqEGww0j', 'auth-rol-dao-tes-3-name-DqEGww0j', 'auth-tenant-dao-test-2-DqEGww0j', 'ams', 'from AuthRoleDaoTest', false, false, 'system', '系统');

-- 父子链：root(30) <- mid(31) <- leaf(32)；以及 root 的第二个子 sibling(33)，全部 t1/ams
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "parent_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('7e4a2d1b-0000-0000-0000-000000000030', 'auth-role-dao-root-DqEGww0j', 'root', 'auth-tenant-dao-test-1-DqEGww0j', 'ams', null, 'from AuthRoleDaoTest', true, false, 'system', '系统'),
    ('7e4a2d1b-0000-0000-0000-000000000031', 'auth-role-dao-mid-DqEGww0j', 'mid', 'auth-tenant-dao-test-1-DqEGww0j', 'ams', '7e4a2d1b-0000-0000-0000-000000000030', 'from AuthRoleDaoTest', true, false, 'system', '系统'),
    ('7e4a2d1b-0000-0000-0000-000000000032', 'auth-role-dao-leaf-DqEGww0j', 'leaf', 'auth-tenant-dao-test-1-DqEGww0j', 'ams', '7e4a2d1b-0000-0000-0000-000000000031', 'from AuthRoleDaoTest', true, false, 'system', '系统'),
    ('7e4a2d1b-0000-0000-0000-000000000033', 'auth-role-dao-sib-DqEGww0j', 'sibling', 'auth-tenant-dao-test-1-DqEGww0j', 'ams', '7e4a2d1b-0000-0000-0000-000000000030', 'from AuthRoleDaoTest', true, false, 'system', '系统');
