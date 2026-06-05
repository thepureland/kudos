-- Fixture for MsgReceiveServiceTest.
-- One instance + one send, then five receive rows:
--   ruser-1: '11'(RECEIVED/unread), '01'(UNREAD), '12'(READ), '21'(DELETED)  -> 2 unread
--   ruser-2: '01'(UNREAD)                                                    -> 1 unread
-- create_time is staggered so the DESC ordering of getReceivesByUserId is observable.

merge into "msg_instance"
    ("id", "template_id", "send_type_dict_code", "event_type_dict_code", "msg_type_dict_code", "tenant_id")
values
    ('b2000000-0000-0000-0000-0000000000a1', null, 'auto', 'user_registered', 'welcome', 'svc-tenant-msg-recv');

merge into "msg_send"
    ("id", "receiver_group_type_dict_code", "instance_id", "msg_type_dict_code", "send_status_dict_code", "tenant_id")
values
    ('c3000000-0000-0000-0000-0000000000a1', 'USER', 'b2000000-0000-0000-0000-0000000000a1', 'welcome', '33', 'svc-tenant-msg-recv');

merge into "msg_receive"
    ("id", "receiver_id", "send_id", "receive_status_dict_code", "create_time", "tenant_id")
values
    ('d4000000-0000-0000-0000-000000000001', 'ruser-1', 'c3000000-0000-0000-0000-0000000000a1', '11', '2026-01-01 10:00:00', 'svc-tenant-msg-recv'),
    ('d4000000-0000-0000-0000-000000000002', 'ruser-1', 'c3000000-0000-0000-0000-0000000000a1', '01', '2026-01-02 10:00:00', 'svc-tenant-msg-recv'),
    ('d4000000-0000-0000-0000-000000000003', 'ruser-1', 'c3000000-0000-0000-0000-0000000000a1', '12', '2026-01-03 10:00:00', 'svc-tenant-msg-recv'),
    ('d4000000-0000-0000-0000-000000000004', 'ruser-1', 'c3000000-0000-0000-0000-0000000000a1', '21', '2026-01-04 10:00:00', 'svc-tenant-msg-recv'),
    ('d4000000-0000-0000-0000-000000000005', 'ruser-2', 'c3000000-0000-0000-0000-0000000000a1', '01', '2026-01-05 10:00:00', 'svc-tenant-msg-recv');
