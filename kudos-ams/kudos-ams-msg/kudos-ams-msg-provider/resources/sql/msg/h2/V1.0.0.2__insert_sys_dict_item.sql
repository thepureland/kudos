--region DML

-- publish_method
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('863b4bde-306c-4ca8-978b-c3669c5bc044', '181c57ec-df00-4844-a79b-5b1019ec25ec', 'email', '电子邮件', 1, null, true),
    ('b7fe6cdc-39b4-40fb-837d-4eb4984d2d71', '181c57ec-df00-4844-a79b-5b1019ec25ec', 'sms', '手机短信', 2, null, true),
    ('cfb5937d-87da-4aa4-a553-4e47a03cd824', '181c57ec-df00-4844-a79b-5b1019ec25ec', 'siteMsg', '站内信', 3, null, true),
    ('a6f7e749-3a2a-4848-8d5d-d6bd8d4a9e0e', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'all_user', '所有用户', 1, null, true);

-- receiver_group_type
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('b8624f7e-01c7-4fac-8a77-grouptype001', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'all_front', '所有前台用户', 1, null, true),
    ('b8624f7e-01c7-4fac-8a77-grouptype001', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'all_back', '所有后台用户', 2, null, true),
    ('b8624f7e-01c7-4fac-8a77-grouptype001', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'online_front', '前台在线用户', 3, null, true),
    ('b8624f7e-01c7-4fac-8a77-grouptype001', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'online_back', '后台在线用户', 4, null, true),
    ('b8624f7e-01c7-4fac-8a77-grouptype001', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'offline_front', '前台不在线用户', 5, null, true),
    ('b8624f7e-01c7-4fac-8a77-grouptype001', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'offline_back', '后台不在线用户', 6, null, true),
    ('b8624f7e-01c7-4fac-8a77-grouptype001', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'dept', '部门', 7, null, true),
    ('55595439-5c3d-4c23-8ff9-grouptype002', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'role', '角色', 8, null, true),
    ('55595439-5c3d-4c23-8ff9-grouptype003', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'tag', '标签', 9, null, true),
    ('55595439-5c3d-4c23-8ff9-grouptype003', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'guest', '游客', 10, null, true),
    ('ea483c96-60f2-45a6-879b-grouptype004', '0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'user', '具体用户', 11, null, true);

-- send_status
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('73f63aea-221f-4e88-8ce6-fef241a07869', '1c147a5b-0543-497d-bcae-221aec84256c', '00', '等待发送', 1, null, true),
    ('30344b3d-14e8-4502-8d73-5c44026a1386', '1c147a5b-0543-497d-bcae-221aec84256c', '01', '取消发送', 2, null, true),
    ('50469de2-3a17-4cec-86bd-8f88c775d4c3', '1c147a5b-0543-497d-bcae-221aec84256c', '11', '已发送给消息队列', 3, null, true),
    ('94abc228-4107-4670-9b2c-43bb08eb1a10', '1c147a5b-0543-497d-bcae-221aec84256c', '21', '发送给消息队列失败', 4, null, true),
    ('69018bce-68b0-486d-a966-bdd5ca70b263', '1c147a5b-0543-497d-bcae-221aec84256c', '22', '最终发送失败', 5, null, true),
    ('e6a86d53-0edb-4cce-819c-76268ffc177b', '1c147a5b-0543-497d-bcae-221aec84256c', '31', '已从消息队列消费', 6, null, true),
    ('a8b39fc9-e82b-44d2-b607-03296caa7880', '1c147a5b-0543-497d-bcae-221aec84256c', '32', '发送完成，但是部分用户发送失败', 7, null, true),
    ('0e26e39f-fc29-47d1-bad7-4a6c66878900', '1c147a5b-0543-497d-bcae-221aec84256c', '33', '发送成功', 8, null, true),

-- tmpl_type
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('64bd32cf-5593-48a5-babb-48736fa6378e', '331dd7f9-77b7-49af-87d0-a1d7046bfb20', 'auto', '自动通知模板', 1, null, true),
    ('6376df77-effb-47ab-ad69-a9956d798324', '331dd7f9-77b7-49af-87d0-a1d7046bfb20', 'manual', '手工信息模板', 2, null, true),

-- receive_status
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('cac126d1-7a4f-4721-aaf4-4dcc6c8f123d', 'd275942e-262b-460a-917e-ec96aab565cc', '11', '已接收', 1, null, true),
    ('377a4e40-74e4-4944-8e50-b9a3425f350a', 'd275942e-262b-460a-917e-ec96aab565cc', '01', '未读', 2, null, true, true),
    ('b37b5917-cf2d-46c5-8d8d-eb4118649922', 'd275942e-262b-460a-917e-ec96aab565cc', '12', '已读', 3, null, true, true),
    ('3d2c1732-96cb-4820-b180-cf4f4d66ead8', 'd275942e-262b-460a-917e-ec96aab565cc', '21', '已删除', 4, null, true);

--endregion DML