-- code/name 均加 SbcCH_ 前缀，避免与其它测试或种子数据主键/唯一约束冲突（sys_system.name 有 unique）
merge into "sys_system" ("code", "name", "remark", "active", "built_in") values
('SbcCH_7a3f9b2c4e5f6_1', 'SbcCH-name-1', null, true, true),
('SbcCH_7a3f9b2c4e5f6_2', 'SbcCH-name-2', null, true, true),
('SbcCH_7a3f9b2c4e5f6_3', 'SbcCH-name-3', null, true, true),
('SbcCH_7a3f9b2c4e5f6_4', 'SbcCH-name-4', null, true, true),
('SbcCH_7a3f9b2c4e5f6_5', 'SbcCH-name-5', null, true, true),
('SbcCH_7a3f9b2c4e5f6_6', 'SbcCH-name-6', null, true, true),
('SbcCH_7a3f9b2c4e5f6_7', 'SbcCH-name-7', null, true, true),
('SbcCH_7a3f9b2c4e5f6_8', 'SbcCH-name-8', null, true, true),
('SbcCH_7a3f9b2c4e5f6_9', 'SbcCH-name-9', null, true, true),
('SbcCH_7a3f9b2c4e5f6_0', 'SbcCH-name-0', null, false, true);
