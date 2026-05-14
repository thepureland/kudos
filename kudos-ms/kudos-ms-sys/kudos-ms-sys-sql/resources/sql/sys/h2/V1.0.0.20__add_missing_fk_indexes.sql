--region DDL
-- 补齐缺失的索引：FK 列若无索引，级联 join / 删除会全表扫；树形列若无索引，递归遍历会逐层扫表。

-- sys_access_rule_ip.parent_rule_id：FK 引用 sys_access_rule(id)，每次按父规则查 IP 列表都会用到
create index if not exists "idx_sys_access_rule_ip_parent_rule_id"
    on "sys_access_rule_ip" ("parent_rule_id");

-- sys_dict_item.dict_id：现有的 uq_sys_dict_item (item_code, dict_id) 以 item_code 为前缀，按 dict_id 单列查询无法命中
create index if not exists "idx_sys_dict_item_dict_id"
    on "sys_dict_item" ("dict_id");

-- sys_dict_item.parent_id：用于树形遍历（SysDictItemService.recursionFindAllChildId）
create index if not exists "idx_sys_dict_item_parent_id"
    on "sys_dict_item" ("parent_id");
--endregion DDL
