--region DML

merge into "sys_dict" ("id", "atomic_service_code", "dict_type", "dict_name", "remark", "built_in") values
    ('181c57ec-df00-4844-a79b-5b1019ec25ec', 'kudos-msg', 'publish_method', 'publish_method', null, true),
    ('0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'kudos-msg', 'receiver_group_type', 'receiver_group_type', null, true),
    ('1c147a5b-0543-497d-bcae-221aec84256c', 'kudos-msg', 'send_status', 'send_status', null, true),
    ('331dd7f9-77b7-49af-87d0-a1d7046bfb20', 'kudos-msg', 'tmpl_type', 'tmpl_type', null, true),
    ('27dcee56-af0c-46c3-b717-f2f3e61f4c84', 'kudos-msg', 'auto_event_type', 'auto_event_type', '系统通知模板事件类型', true),
    ('3ff26ac3-6c1c-4334-a83b-d8042ccf3b8c', 'kudos-msg', 'manual_event_type', 'manual_event_type', '手动通知模板事件类型', true),
    ('9a70bb50-b330-42f9-9658-f8d8cd5b1679', 'kudos-msg', 'params', 'params', '通知参数', true),
    ('d275942e-262b-460a-917e-ec96aab565cc', 'kudos-msg', 'receive_status', 'receive_status', null, true);

--endregion DML


