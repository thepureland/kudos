-- 测试数据：SysTenantLocaleDaoTest（每条 id 唯一；tenant_locale 每行 (tenant_id,locale_code) 不重复）

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in") values
('40000000-0000-0000-0000-000000003459', 'svc-tenant-tlang-dao-test-1', null, null, 'from SysTenantLocaleDaoTest', true, false),
('40000000-0000-0000-0000-000000003460', 'svc-tenant-tlang-dao-test-2', null, null, 'from SysTenantLocaleDaoTest', true, false);

merge into "sys_tenant_locale" ("id", "tenant_id", "locale_code") values
('40000000-0000-0000-0000-000000003459', '40000000-0000-0000-0000-000000003459', 'zh-CN'),
('40000000-0000-0000-0000-000000003460', '40000000-0000-0000-0000-000000003459', 'en-US'),
('40000000-0000-0000-0000-000000003461', '40000000-0000-0000-0000-000000003460', 'zh-CN');
