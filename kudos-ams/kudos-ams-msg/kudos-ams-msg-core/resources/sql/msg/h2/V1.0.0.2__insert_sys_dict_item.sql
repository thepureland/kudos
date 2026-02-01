--region DML

-- publish_method
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('863b4bde-306c-4ca8-978b-c3669c5bc044', '181c57ec-df00-4844-a79b-5b1019ec25ec', 'email', 'publish_method.email', 1, '电子邮件', true),
    ('b7fe6cdc-39b4-40fb-837d-4eb4984d2d71', '181c57ec-df00-4844-a79b-5b1019ec25ec', 'sms', 'publish_method.sms', 2, '手机短信', true),
    ('cfb5937d-87da-4aa4-a553-4e47a03cd824', '181c57ec-df00-4844-a79b-5b1019ec25ec', 'siteMsg', 'publish_method.siteMsg', 3, '站内信', true),
    ('a6f7e749-3a2a-4848-8d5d-d6bd8d4a9e0e', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'all_user', 'publish_method.all_user', 1, '所有用户', true);

-- receiver_group_type
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('b8624f7e-01c7-4fac-8a77-grouptype001', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'all_front', 'receiver_group_type.all_front', 1, '所有前台用户', true),
    ('b8624f7e-01c7-4fac-8a77-grouptype002', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'all_back', 'receiver_group_type.all_back', 2, '所有后台用户', true),
    ('b8624f7e-01c7-4fac-8a77-grouptype003', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'online_front', 'receiver_group_type.online_front', 3, '前台在线用户', true),
    ('b8624f7e-01c7-4fac-8a77-grouptype004', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'online_back', 'receiver_group_type.online_back', 4, '后台在线用户', true),
    ('b8624f7e-01c7-4fac-8a77-grouptype005', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'offline_front', 'receiver_group_type.offline_front', 5, '前台不在线用户', true),
    ('b8624f7e-01c7-4fac-8a77-grouptype006', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'offline_back', 'receiver_group_type.offline_back', 6, '后台不在线用户', true),
    ('b8624f7e-01c7-4fac-8a77-grouptype007', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'dept', 'receiver_group_type.dept', 7, '部门', true),
    ('55595439-5c3d-4c23-8ff9-grouptype008', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'role', 'receiver_group_type.role', 8, '角色', true),
    ('55595439-5c3d-4c23-8ff9-grouptype009', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'tag', 'receiver_group_type.tag', 9, '标签', true),
    ('55595439-5c3d-4c23-8ff9-grouptype010', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'guest', 'receiver_group_type.guest', 10, '游客', true),
    ('ea483c96-60f2-45a6-879b-grouptype011', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'user', 'receiver_group_type.user', 11, '具体用户', true);

-- send_status
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('73f63aea-221f-4e88-8ce6-fef241a07869', '1c147a5b-0543-497d-bcae-221aec84256c', '00', 'send_status.00', 1, '等待发送', true),
    ('30344b3d-14e8-4502-8d73-5c44026a1386', '1c147a5b-0543-497d-bcae-221aec84256c', '01', 'send_status.01', 2, '取消发送', true),
    ('50469de2-3a17-4cec-86bd-8f88c775d4c3', '1c147a5b-0543-497d-bcae-221aec84256c', '11', 'send_status.11', 3, '已发送给消息队列', true),
    ('94abc228-4107-4670-9b2c-43bb08eb1a10', '1c147a5b-0543-497d-bcae-221aec84256c', '21', 'send_status.21', 4, '发送给消息队列失败', true),
    ('69018bce-68b0-486d-a966-bdd5ca70b263', '1c147a5b-0543-497d-bcae-221aec84256c', '22', 'send_status.22', 5, '最终发送失败', true),
    ('e6a86d53-0edb-4cce-819c-76268ffc177b', '1c147a5b-0543-497d-bcae-221aec84256c', '31', 'send_status.31', 6, '已从消息队列消费', true),
    ('a8b39fc9-e82b-44d2-b607-03296caa7880', '1c147a5b-0543-497d-bcae-221aec84256c', '32', 'send_status.32', 7, '发送完成，但是部分用户发送失败', true),
    ('0e26e39f-fc29-47d1-bad7-4a6c66878900', '1c147a5b-0543-497d-bcae-221aec84256c', '33', 'send_status.33', 8, '发送成功', true),

-- tmpl_type
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('64bd32cf-5593-48a5-babb-48736fa6378e', '331dd7f9-77b7-49af-87d0-a1d7046bfb20', 'auto', 'tmpl_type.auto', 1, '自动通知模板', true),
    ('6376df77-effb-47ab-ad69-a9956d798324', '331dd7f9-77b7-49af-87d0-a1d7046bfb20', 'manual', 'tmpl_type.manual', 2, '手工信息模板', true),

-- receive_status
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('cac126d1-7a4f-4721-aaf4-4dcc6c8f123d', 'd275942e-262b-460a-917e-ec96aab565cc', '11', 'receive_status.11', 1, '已接收', true),
    ('377a4e40-74e4-4944-8e50-b9a3425f350a', 'd275942e-262b-460a-917e-ec96aab565cc', '01', 'receive_status.01', 2, '未读', true),
    ('b37b5917-cf2d-46c5-8d8d-eb4118649922', 'd275942e-262b-460a-917e-ec96aab565cc', '12', 'receive_status.12', 3, '已读', true),
    ('3d2c1732-96cb-4820-b180-cf4f4d66ead8', 'd275942e-262b-460a-917e-ec96aab565cc', '21', 'receive_status.21', 4, '已删除', true);

--endregion DML
