-- 权限点注册表用例：覆盖"哪一行胜出"的判定规则。
-- 刻意让行的插入顺序与期望胜出顺序相反——判定结果绝不能依赖表里的行序。

-- 1) 同一路径同时有"限方法"和"不限方法"两行：限方法者胜
merge into "sys_resource" ("id", "name", "permission_code", "url", "resource_type_dict_code", "sub_system_code", "active", "built_in") values
    ('7e10a001-0000-0000-0000-000000000001', 'ppr.any.role.bindUsers', 'ppr:role:bindUsers:any', '/api/admin/ppr/role/bindUsers', '2', 'ppr-sub-system', true, false),
    ('7e10a001-0000-0000-0000-000000000002', 'ppr.post.role.bindUsers', 'ppr:role:bindUsers', 'POST:/api/admin/ppr/role/bindUsers', '2', 'ppr-sub-system', true, false);

-- 2) 两条都能匹配时，模式更长者胜（先插短的）
merge into "sys_resource" ("id", "name", "permission_code", "url", "resource_type_dict_code", "sub_system_code", "active", "built_in") values
    ('7e10a001-0000-0000-0000-000000000003', 'ppr.subtree', 'ppr:group:subtree', '/api/admin/ppr/group/**', '2', 'ppr-sub-system', true, false),
    ('7e10a001-0000-0000-0000-000000000004', 'ppr.exact', 'ppr:group:listUserIds', '/api/admin/ppr/group/listUserIds', '2', 'ppr-sub-system', true, false);

-- 3) 停用的权限点不参与解析
merge into "sys_resource" ("id", "name", "permission_code", "url", "resource_type_dict_code", "sub_system_code", "active", "built_in") values
    ('7e10a001-0000-0000-0000-000000000005', 'ppr.inactive', 'ppr:dead:endpoint', '/api/admin/ppr/dead/endpoint', '2', 'ppr-sub-system', false, false);

-- 4) 有 url 但没有权限编码的行不是权限点（编码化迁移期的中间态）
merge into "sys_resource" ("id", "name", "url", "resource_type_dict_code", "sub_system_code", "active", "built_in") values
    ('7e10a001-0000-0000-0000-000000000006', 'ppr.uncoded', '/api/admin/ppr/uncoded/thing', '2', 'ppr-sub-system', true, false);

-- 5) 路径里含冒号：不能被误当成方法限定符
merge into "sys_resource" ("id", "name", "permission_code", "url", "resource_type_dict_code", "sub_system_code", "active", "built_in") values
    ('7e10a001-0000-0000-0000-000000000007', 'ppr.colon', 'ppr:odd:colonPath', '/api/admin/ppr/odd/we:ird', '2', 'ppr-sub-system', true, false);
