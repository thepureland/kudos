-- Fixture for MsgReceiverGroupServiceTest.
-- Three receiver groups (the type_dict_code column is uniquely indexed, so each row uses a distinct type):
--   g1..01: type 'USER', active=true
--   g1..02: type 'DEPT', active=true
--   g1..03: type 'ROLE', active=false   (must be excluded by fetchActiveReceiverGroups)

merge into "msg_receiver_group"
    ("id", "receiver_group_type_dict_code", "define_table", "name_column", "remark", "active", "built_in")
values
    ('a1000000-0000-0000-0000-000000000001', 'USER', 'sys_user', 'user_name', 'r1', true, false),
    ('a1000000-0000-0000-0000-000000000002', 'DEPT', 'org_dept', 'dept_name', 'r2', true, false),
    ('a1000000-0000-0000-0000-000000000003', 'ROLE', 'auth_role', 'role_name', 'r3', false, false);
