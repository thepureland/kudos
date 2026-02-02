-- sys_access_rule: 每条 (tenant_id, system_code) 一行，id 唯一。(tenantId-2, subSys-a) 的 id 为 8026f3ac-563b-4545-88dc-b8f70ea44847，供 sync 用例使用
merge into "sys_access_rule" ("id", "tenant_id", "system_code", "rule_type_dict_code", "remark", "active", "built_in") values
('8026f3ac-563b-4545-88dc-b8f70ea44848', 'tenantId-1', 'subSys-a', '0', null, true, false),
('8026f3ac-563b-4545-88dc-b8f70ea44847', 'tenantId-2', 'subSys-a', '1', null, true, false),
('8026f3ac-563b-4545-88dc-b8f70ea44849', 'tenantId-3', 'subSys-a', '2', null, true, false),
('8026f3ac-563b-4545-88dc-b8f70ea4484a', 'tenantId-4', 'subSys-a', '3', null, true, false),
('8026f3ac-563b-4545-88dc-b8f70ea4484b', 'tenantId-5', 'subSys-b', '0', null, true, false),
('8026f3ac-563b-4545-88dc-b8f70ea4484c', 'tenantId-6', 'subSys-b', '0', null, true, false),
('8026f3ac-563b-4545-88dc-b8f70ea4484d', null, 'subSys-c', '0', null, true, false),
('8026f3ac-563b-4545-88dc-b8f70ea4484e', null, 'subSys-d', '0', null, true, false),
('8026f3ac-563b-4545-88dc-b8f70ea4484f', null, 'subSys-e', '0', null, true, false),
('8026f3ac-563b-4545-88dc-b8f70ea44850', null, 'subSys-f', '0', null, false, false);

-- sys_access_rule_ip: 每条 id 唯一。3a443825-4896-49e4-a304-e4e2ddad4847 的 parent 为 (tenantId-2, subSys-a) 的规则 id
merge into "sys_access_rule_ip" ("id", "ip_start", "ip_end", "ip_type_dict_code", "expiration_time", "parent_rule_id", "remark", "active", "built_in") values
('3a443825-4896-49e4-a304-e4e2ddad4847', 3232235521, 3232235648, '1', null, '8026f3ac-563b-4545-88dc-b8f70ea44847', null, true, false),
('3a443825-4896-49e4-a304-e4e2ddad4848', 3232235649, 3232235649, '1', null, '8026f3ac-563b-4545-88dc-b8f70ea44847', null, true, false),
('3a443825-4896-49e4-a304-e4e2ddad4849', 3232235659, 3232235659, '1', null, '8026f3ac-563b-4545-88dc-b8f70ea44848', null, true, false),
('3a443825-4896-49e4-a304-e4e2ddad484a', 3232235660, 3232235720, '2', null, '8026f3ac-563b-4545-88dc-b8f70ea4484d', null, true, false);
