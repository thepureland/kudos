-- Fixture for MsgUnreceivedServiceTest.
-- One instance + one send, then two msg_unreceived rows under that send:
--   e5..01: resolved=false, retry_count=2  (for bumpRetry / resolve / findUnresolved)
--   e5..02: resolved=true                  (must be excluded by findUnresolvedBySend)

merge into "msg_instance"
    ("id", "template_id", "send_type_dict_code", "event_type_dict_code", "msg_type_dict_code", "tenant_id")
values
    ('b2000000-0000-0000-0000-0000000000b1', null, 'auto', 'user_registered', 'welcome', 'svc-tenant-msg-unrecv');

merge into "msg_send"
    ("id", "receiver_group_type_dict_code", "instance_id", "msg_type_dict_code", "send_status_dict_code", "tenant_id")
values
    ('c3000000-0000-0000-0000-0000000000b1', 'USER', 'b2000000-0000-0000-0000-0000000000b1', 'welcome', '33', 'svc-tenant-msg-unrecv');

merge into "msg_unreceived"
    ("id", "receiver_id", "send_id", "publish_method_dict_code", "fail_reason",
     "retry_count", "resolved", "create_time", "tenant_id")
values
    ('e5000000-0000-0000-0000-000000000001', 'uu-1', 'c3000000-0000-0000-0000-0000000000b1', 'EMAIL', 'CHANNEL_REJECT',
     2, false, '2026-01-01 10:00:00', 'svc-tenant-msg-unrecv'),
    ('e5000000-0000-0000-0000-000000000002', 'uu-2', 'c3000000-0000-0000-0000-0000000000b1', 'EMAIL', 'NO_CONTACT',
     0, true, '2026-01-01 10:00:00', 'svc-tenant-msg-unrecv');
