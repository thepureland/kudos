merge into "sys_dict" ("id", "module_code", "dict_type", "dict_name", "remark", "active", "built_in")
    values ('78139ed2-dbce-47fa-ac0d-111111111111', 'kudos-sys', 'dict_type-11', '数据源用途', null, true, false),
           ('78139ed2-dbce-47fa-ac0d-222222222222', 'kudos-sys', 'dict_type-22', '数据源类型', null, true, false),
           ('78139ed2-dbce-47fa-ac0d-333333333333', 'kudos-sys', 'dict_type-33', '资源类型', null, true, false),
           ('78139ed2-dbce-47fa-ac0d-444444444444', 'kudos-user', 'dict_type-44', '组织类型', '部门类型', true, true),
           ('78139ed2-dbce-47fa-ac0d-555555555555', 'kudos-user', 'dict_type-55', '性别', null, false, true);


merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "active", "built_in")
    values ('e8ff3f9a-a57a-4183-953d-fe80c12fc777', '78139ed2-dbce-47fa-ac0d-111111111111', 'local', '本地数据源', 1,
            null, true, true),
           ('b9364152-1127-4129-9f65-101be811bddd', '78139ed2-dbce-47fa-ac0d-111111111111', 'remote', '远程数据源', 2,
            null, true, true),
           ('c46091d2-945c-4440-b103-ac58a7aec999', '78139ed2-dbce-47fa-ac0d-111111111111', 'report', '报表数据源', 3,
            null, false, true),
           ('b0e0bf5c-d2f8-46ee-9c1f-14049645d222', '78139ed2-dbce-47fa-ac0d-111111111111', 'readonly', '只读数据源', 4,
            null, true, true),
           ('8aabaa7f-6d19-4d8a-8aed-a9f8ca553eee', '78139ed2-dbce-47fa-ac0d-222222222222', 'hikariCP', 'hikariCP', 1,
            null, true, true),
           ('d2e7c962-d0ca-43a5-b722-e1878dfa1555', '78139ed2-dbce-47fa-ac0d-333333333333', '2', '功能', 2, null, true,
            true),
           ('dd359907-6587-46d6-82a6-a5a3dd038ccc', '78139ed2-dbce-47fa-ac0d-333333333333', '1', '菜单', 1, null, true,
            true),
           ('049132a6-daac-4434-af36-909477396ddd', '78139ed2-dbce-47fa-ac0d-555555555555', 'M', '男', 1, null, true,
            true),
           ('04626227-0ac0-49a2-8036-241cd017a888', '78139ed2-dbce-47fa-ac0d-555555555555', 'W', '女', 2, null, true,
            true);

