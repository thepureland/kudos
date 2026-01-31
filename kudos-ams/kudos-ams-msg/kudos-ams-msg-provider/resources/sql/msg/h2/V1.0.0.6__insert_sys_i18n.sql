--region DML

-- dict
merge into "sys_i18n" ("id", "locale", "module_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('bba0ce4a-35c0-4497-8a24-27d976adf6f2', 'zh_CN', 'kudos-msg', 'dict', 'publish_method', '发布方式', true),
    ('2f8c652e-b9e4-4ff2-8845-c66463f44979', 'zh_TW', 'kudos-msg', 'dict', 'publish_method', '發布方式', true),
    ('064dc55c-00f2-445a-bd2d-3488a9115613', 'en_US', 'kudos-msg', 'dict', 'publish_method', 'Publish Method', true),

    ('e0675547-77b3-4a17-8143-373b8baf3095', 'zh_CN', 'kudos-msg', 'dict', 'receiver_group_type', '接收者群组类型', true),
    ('3af2c6f2-16c4-4a26-9a20-7402eedce2cd', 'zh_TW', 'kudos-msg', 'dict', 'receiver_group_type', '接收者群組類型', true),
    ('3a308a2f-f506-445a-a1fe-27f7108f3d25', 'en_US', 'kudos-msg', 'dict', 'receiver_group_type', 'Receiver Group Type', true),

    ('22a5cd88-3437-45d0-b900-8f8673f666b4', 'zh_CN', 'kudos-msg', 'dict', 'send_status', '发送状态', true),
    ('2460a2b1-03d2-4ad9-adff-3fab7bb1890f', 'zh_TW', 'kudos-msg', 'dict', 'send_status', '發送狀態', true),
    ('f7557a6d-918d-4ee9-aa90-06429661f31e', 'en_US', 'kudos-msg', 'dict', 'send_status', 'Send Status', true),

    ('9c5635f3-d63f-4aa2-a8b8-25f460781355', 'zh_CN', 'kudos-msg', 'dict', 'tmpl_type', '模板类型', true),
    ('ad511c76-d567-470d-a35c-defe597e4066', 'zh_TW', 'kudos-msg', 'dict', 'tmpl_type', '模板類型', true),
    ('f987a54d-9c7b-45eb-a2bc-d6beec88850c', 'en_US', 'kudos-msg', 'dict', 'tmpl_type', 'Template Type', true),

    ('a173e264-b19a-4cbf-b47b-75bcc7ac2c69', 'zh_CN', 'kudos-msg', 'dict', 'auto_event_type', '系统通知模板事件类型', true),
    ('d8ea0b62-9fe1-4b75-9f99-38df9f697c16', 'zh_TW', 'kudos-msg', 'dict', 'auto_event_type', '系統通知模板事件類型', true),
    ('a54d8c55-3910-49de-9019-12f8f5e49921', 'en_US', 'kudos-msg', 'dict', 'auto_event_type', 'System Notification Template Event Type', true),

    ('343e6f58-1cdf-4939-b475-b30e7cd615ee', 'zh_CN', 'kudos-msg', 'dict', 'manual_event_type', '手动通知模板事件类型', true),
    ('6bd8c248-fdfd-4519-ba9d-389eac7d883b', 'zh_TW', 'kudos-msg', 'dict', 'manual_event_type', '手動通知模板事件類型', true),
    ('3bf20c4b-9d26-4a78-8b17-4675b596b2d5', 'en_US', 'kudos-msg', 'dict', 'manual_event_type', 'Manual Notification Template Event Type', true),

    ('fbe5d998-5aed-439e-9f8d-4ef210ea6f4c', 'zh_CN', 'kudos-msg', 'dict', 'params', '通知参数', true),
    ('e0fc86b4-2a99-43eb-b002-261e9131b655', 'zh_TW', 'kudos-msg', 'dict', 'params', '通知參數', true),
    ('5a8364b3-6e38-493c-a923-8eca5a6000ac', 'en_US', 'kudos-msg', 'dict', 'params', 'Notification Parameters', true),

    ('8f8c4384-0ccd-4548-b4d0-7c744d47d415', 'zh_CN', 'kudos-msg', 'dict', 'receive_status', '接收状态', true),
    ('c99b7c00-3339-4f98-987b-7fb3af4a2800', 'zh_TW', 'kudos-msg', 'dict', 'receive_status', '接收狀態', true),
    ('a131b33f-7d6a-4613-8789-ecc93c1e8f12', 'en_US', 'kudos-msg', 'dict', 'receive_status', 'Receive Status', true);

-- dict-item publish_method
merge into "sys_i18n" ("id", "locale", "module_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('b008a406-2d7d-4887-972a-4f7968507476', 'zh_CN', 'kudos-msg', 'dict-item', 'publish_method.email', '电子邮件', true),
    ('e87cef81-2e67-40a7-9cef-afca5df20b48', 'zh_TW', 'kudos-msg', 'dict-item', 'publish_method.email', '電子郵件', true),
    ('22822f50-b611-423e-97e1-8469e21f7699', 'en_US', 'kudos-msg', 'dict-item', 'publish_method.email', 'Email', true),

    ('2a1175ff-c89b-444d-8f5e-549a4f40d7d2', 'zh_CN', 'kudos-msg', 'dict-item', 'publish_method.sms', '手机短信', true),
    ('ec8504a9-51f2-45c7-9d13-322376446cfc', 'zh_TW', 'kudos-msg', 'dict-item', 'publish_method.sms', '手機簡訊', true),
    ('fd102573-9080-45c7-abc4-7b51b976462b', 'en_US', 'kudos-msg', 'dict-item', 'publish_method.sms', 'SMS', true),

    ('24777cd1-1fe0-4155-a576-7693ac7d6cd9', 'zh_CN', 'kudos-msg', 'dict-item', 'publish_method.siteMsg', '站内信', true),
    ('b58b5b85-c3e1-4847-9977-1e8854c7ab7b', 'zh_TW', 'kudos-msg', 'dict-item', 'publish_method.siteMsg', '站內信', true),
    ('fbe38d57-5a19-42b2-b4d6-08def533e933', 'en_US', 'kudos-msg', 'dict-item', 'publish_method.siteMsg', 'In-site Message', true),

    ('0e5b9aad-2b55-4928-a013-5e234b190adc', 'zh_CN', 'kudos-msg', 'dict-item', 'publish_method.all_user', '所有用户', true),
    ('8e22afd8-a665-47c8-baea-ef6d1b111572', 'zh_TW', 'kudos-msg', 'dict-item', 'publish_method.all_user', '所有使用者', true),
    ('67e2b7ad-2afa-42e1-8c17-b284fd79983b', 'en_US', 'kudos-msg', 'dict-item', 'publish_method.all_user', 'All Users', true);

-- dict-item receiver_group_type
merge into "sys_i18n" ("id", "locale", "module_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('535f3775-87e5-44f0-bcd0-5fe2c48a08f1', 'zh_CN', 'kudos-msg', 'dict-item', 'receiver_group_type.all_front', '所有前台用户', true),
    ('d394ed76-ad99-4cc6-86fe-9350558233d5', 'zh_TW', 'kudos-msg', 'dict-item', 'receiver_group_type.all_front', '所有前台使用者', true),
    ('61a0e039-5824-4390-9802-535e847e71d7', 'en_US', 'kudos-msg', 'dict-item', 'receiver_group_type.all_front', 'All Front-end Users', true),

    ('291444ec-6686-4b5d-8022-7de4a0b8cf6b', 'zh_CN', 'kudos-msg', 'dict-item', 'receiver_group_type.all_back', '所有后台用户', true),
    ('325a4f6e-b7f2-403a-b8f1-93f86b7f0c32', 'zh_TW', 'kudos-msg', 'dict-item', 'receiver_group_type.all_back', '所有後台使用者', true),
    ('0b92c06f-d833-4e67-b204-2ad5f75e5a23', 'en_US', 'kudos-msg', 'dict-item', 'receiver_group_type.all_back', 'All Back-end Users', true),

    ('938f6a49-ee3f-41aa-be47-6805f82a1c6f', 'zh_CN', 'kudos-msg', 'dict-item', 'receiver_group_type.online_front', '前台在线用户', true),
    ('f357043f-d0e7-4d37-8fd5-479c77cd4714', 'zh_TW', 'kudos-msg', 'dict-item', 'receiver_group_type.online_front', '前台在線使用者', true),
    ('a6625e20-9437-490b-b917-1804711e70b7', 'en_US', 'kudos-msg', 'dict-item', 'receiver_group_type.online_front', 'Online Front-end Users', true),

    ('fa623fdf-fab5-46c7-a675-394568ba45f2', 'zh_CN', 'kudos-msg', 'dict-item', 'receiver_group_type.online_back', '后台在线用户', true),
    ('3fb2623c-fc63-48f2-9b34-db740ad0b0de', 'zh_TW', 'kudos-msg', 'dict-item', 'receiver_group_type.online_back', '後台在線使用者', true),
    ('91f254f3-e4cb-43c3-88d8-0c331d4c50d3', 'en_US', 'kudos-msg', 'dict-item', 'receiver_group_type.online_back', 'Online Back-end Users', true),

    ('03fcb2fd-7bd0-48a1-bf37-7aeaf7d0066c', 'zh_CN', 'kudos-msg', 'dict-item', 'receiver_group_type.offline_front', '前台不在线用户', true),
    ('36f24f10-58c0-41dd-8760-7283400483e3', 'zh_TW', 'kudos-msg', 'dict-item', 'receiver_group_type.offline_front', '前台不在線使用者', true),
    ('088a63d5-876e-4633-8db8-1213cea614d8', 'en_US', 'kudos-msg', 'dict-item', 'receiver_group_type.offline_front', 'Offline Front-end Users', true),

    ('ba57c80e-4fd9-453f-ba38-2b5a249b6513', 'zh_CN', 'kudos-msg', 'dict-item', 'receiver_group_type.offline_back', '后台不在线用户', true),
    ('5c6e6ae1-c0cc-4b10-a0e5-878495f109cd', 'zh_TW', 'kudos-msg', 'dict-item', 'receiver_group_type.offline_back', '後台不在線使用者', true),
    ('4a71af09-8bba-4378-9eeb-87af8bce53b4', 'en_US', 'kudos-msg', 'dict-item', 'receiver_group_type.offline_back', 'Offline Back-end Users', true),

    ('fcf09424-c7ce-4dfb-b5cb-d117f2038258', 'zh_CN', 'kudos-msg', 'dict-item', 'receiver_group_type.dept', '部门', true),
    ('ca8ac5f4-5a83-42c8-982d-38e90273b277', 'zh_TW', 'kudos-msg', 'dict-item', 'receiver_group_type.dept', '部門', true),
    ('0a481764-180d-48c0-9c67-7c83e5af770b', 'en_US', 'kudos-msg', 'dict-item', 'receiver_group_type.dept', 'Department', true),

    ('7d3d3f79-d0a0-4bad-810e-59205428dc67', 'zh_CN', 'kudos-msg', 'dict-item', 'receiver_group_type.role', '角色', true),
    ('7e631296-2242-4ede-b278-a8e2d5d38406', 'zh_TW', 'kudos-msg', 'dict-item', 'receiver_group_type.role', '角色', true),
    ('1595ff8d-39e2-463c-9cce-75ee025ad30c', 'en_US', 'kudos-msg', 'dict-item', 'receiver_group_type.role', 'Role', true),

    ('5e6c1cf4-e91e-4a50-9d5b-e2d3f5ae77c4', 'zh_CN', 'kudos-msg', 'dict-item', 'receiver_group_type.tag', '标签', true),
    ('7fd3a14e-d08a-4723-992c-d5f4b5d07208', 'zh_TW', 'kudos-msg', 'dict-item', 'receiver_group_type.tag', '標籤', true),
    ('37a7432b-0807-4cef-97bd-3349fa203756', 'en_US', 'kudos-msg', 'dict-item', 'receiver_group_type.tag', 'Tag', true),

    ('e4163ab5-ab5e-4a7a-aa8a-0cab11a45c19', 'zh_CN', 'kudos-msg', 'dict-item', 'receiver_group_type.guest', '游客', true),
    ('bb14ef1b-3136-4c68-aa75-45dc017f4747', 'zh_TW', 'kudos-msg', 'dict-item', 'receiver_group_type.guest', '訪客', true),
    ('444b4cb9-928c-4f97-815b-b557a40a6290', 'en_US', 'kudos-msg', 'dict-item', 'receiver_group_type.guest', 'Guest', true),

    ('edb466b2-850f-4c40-850c-984c4b0919e4', 'zh_CN', 'kudos-msg', 'dict-item', 'receiver_group_type.user', '具体用户', true),
    ('60d0fc8b-444e-4eb1-8b8f-e8f075e8ffc1', 'zh_TW', 'kudos-msg', 'dict-item', 'receiver_group_type.user', '特定使用者', true),
    ('1e59344b-44c7-45de-9a2c-903a31f4d1e3', 'en_US', 'kudos-msg', 'dict-item', 'receiver_group_type.user', 'Specific User', true);

-- dict-item send_status
merge into "sys_i18n" ("id", "locale", "module_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('6fafc84b-e05d-4c5a-922e-a680a5db86ca', 'zh_CN', 'kudos-msg', 'dict-item', 'send_status.00', '等待发送', true),
    ('f8784014-ca7f-4720-a99f-162bcee34dd0', 'zh_TW', 'kudos-msg', 'dict-item', 'send_status.00', '等待發送', true),
    ('9df65850-478d-47c1-8c77-967bf95ed499', 'en_US', 'kudos-msg', 'dict-item', 'send_status.00', 'Pending Send', true),

    ('8aa74c85-8f8c-4899-8eaf-d1ed2468356c', 'zh_CN', 'kudos-msg', 'dict-item', 'send_status.01', '取消发送', true),
    ('3b5f2e83-f17c-4d76-b935-a1cf5817c433', 'zh_TW', 'kudos-msg', 'dict-item', 'send_status.01', '取消發送', true),
    ('92dda25c-d3f6-4e9f-9ec4-360abade6e3f', 'en_US', 'kudos-msg', 'dict-item', 'send_status.01', 'Send Canceled', true),

    ('bd610efc-29f9-4185-a17f-faec6cd1dfb0', 'zh_CN', 'kudos-msg', 'dict-item', 'send_status.11', '已发送给消息队列', true),
    ('88825a88-d579-479e-b9c3-86a61f7e4d22', 'zh_TW', 'kudos-msg', 'dict-item', 'send_status.11', '已發送至訊息佇列', true),
    ('bf7a4852-e897-4db8-a473-6bb800fbc8cc', 'en_US', 'kudos-msg', 'dict-item', 'send_status.11', 'Sent to Message Queue', true),

    ('65c574b7-ea8a-412c-a631-bb6dd0ea3d76', 'zh_CN', 'kudos-msg', 'dict-item', 'send_status.21', '发送给消息队列失败', true),
    ('8fae640d-2d54-4015-bbc5-9b4d0605e81c', 'zh_TW', 'kudos-msg', 'dict-item', 'send_status.21', '發送至訊息佇列失敗', true),
    ('00a7c4cf-dca1-4c75-97db-fa3570f12852', 'en_US', 'kudos-msg', 'dict-item', 'send_status.21', 'Failed to Send to Message Queue', true),

    ('1b73d261-adad-4ba6-a0c6-cf2327a95958', 'zh_CN', 'kudos-msg', 'dict-item', 'send_status.22', '最终发送失败', true),
    ('51f8c6be-6e87-436c-abfb-17bff04b53a4', 'zh_TW', 'kudos-msg', 'dict-item', 'send_status.22', '最終發送失敗', true),
    ('0b1c03a5-06ef-461e-ac44-53929d7853ab', 'en_US', 'kudos-msg', 'dict-item', 'send_status.22', 'Final Send Failed', true),

    ('786251cf-78b8-4f56-aecf-ffd832f12863', 'zh_CN', 'kudos-msg', 'dict-item', 'send_status.31', '已从消息队列消费', true),
    ('5607328e-d0c1-4efa-89e5-eb3e1a6dfb3c', 'zh_TW', 'kudos-msg', 'dict-item', 'send_status.31', '已從訊息佇列消費', true),
    ('64ff93b2-6ae9-4056-b921-5ee79651ad3f', 'en_US', 'kudos-msg', 'dict-item', 'send_status.31', 'Consumed from Message Queue', true),

    ('8b3a6848-f1ce-4d72-a536-15c9b50ecf67', 'zh_CN', 'kudos-msg', 'dict-item', 'send_status.32', '发送完成，但是部分用户发送失败', true),
    ('81c28786-03f2-41d0-b510-551c60e7f8b8', 'zh_TW', 'kudos-msg', 'dict-item', 'send_status.32', '發送完成，但部分使用者發送失敗', true),
    ('c688859f-1b12-4775-80cc-34a0232bc389', 'en_US', 'kudos-msg', 'dict-item', 'send_status.32', 'Sent, but Some Users Failed', true),

    ('e410bcac-a775-4635-86aa-84a066991d99', 'zh_CN', 'kudos-msg', 'dict-item', 'send_status.33', '发送成功', true),
    ('46848f2c-3053-491e-92b0-261709f3d65a', 'zh_TW', 'kudos-msg', 'dict-item', 'send_status.33', '發送成功', true),
    ('3f87a7d5-b0e3-4771-a51d-07963ebb9f4b', 'en_US', 'kudos-msg', 'dict-item', 'send_status.33', 'Sent Successfully', true);

-- dict-item tmpl_type
merge into "sys_i18n" ("id", "locale", "module_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('9ae2f8ab-70f0-4e1a-975a-f28fb85681bf', 'zh_CN', 'kudos-msg', 'dict-item', 'tmpl_type.auto', '自动通知模板', true),
    ('4b534332-33a3-4a43-8cbb-9cd5934e36d3', 'zh_TW', 'kudos-msg', 'dict-item', 'tmpl_type.auto', '自動通知範本', true),
    ('d0ab9226-7244-49b4-b595-cf4c78b712cb', 'en_US', 'kudos-msg', 'dict-item', 'tmpl_type.auto', 'Automatic Notification Template', true),

    ('13ef698a-dd83-443a-a9a8-6d6ed6973f37', 'zh_CN', 'kudos-msg', 'dict-item', 'tmpl_type.manual', '手工信息模板', true),
    ('27261216-6e16-4665-acbd-361374f13901', 'zh_TW', 'kudos-msg', 'dict-item', 'tmpl_type.manual', '手動訊息範本', true),
    ('bf7cb992-212e-4846-8e96-db27470e378e', 'en_US', 'kudos-msg', 'dict-item', 'tmpl_type.manual', 'Manual Message Template', true);

-- dict-item receive_status
merge into "sys_i18n" ("id", "locale", "module_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('bf1f1dd3-ed05-4b84-919b-42dad4e69f03', 'zh_CN', 'kudos-msg', 'dict-item', 'receive_status.11', '已接收', true),
    ('e3e48411-a5d8-4e99-8060-be86dee6a39d', 'zh_TW', 'kudos-msg', 'dict-item', 'receive_status.11', '已接收', true),
    ('deef0f66-814f-4c6b-83d9-853b4c535450', 'en_US', 'kudos-msg', 'dict-item', 'receive_status.11', 'Received', true),

    ('6c538905-1ba0-4592-a042-7235acb59de4', 'zh_CN', 'kudos-msg', 'dict-item', 'receive_status.01', '未读', true),
    ('000459e5-2780-4011-b278-159c3edc56bf', 'zh_TW', 'kudos-msg', 'dict-item', 'receive_status.01', '未讀', true),
    ('2c58aece-35d3-4fb9-9e48-07cf638ed0e2', 'en_US', 'kudos-msg', 'dict-item', 'receive_status.01', 'Unread', true),

    ('b37262be-ba06-41a9-ba3f-028ea1ee0b76', 'zh_CN', 'kudos-msg', 'dict-item', 'receive_status.12', '已读', true),
    ('da311a87-e60f-4ce2-8365-c5864626daa9', 'zh_TW', 'kudos-msg', 'dict-item', 'receive_status.12', '已讀', true),
    ('247e8cbf-a4bd-43a1-828b-f0188b1bb49c', 'en_US', 'kudos-msg', 'dict-item', 'receive_status.12', 'Read', true),

    ('93ef1700-3564-413c-8f3b-a26dc6331275', 'zh_CN', 'kudos-msg', 'dict-item', 'receive_status.21', '已删除', true),
    ('14216954-e33e-4344-a83c-29df74ac9ffe', 'zh_TW', 'kudos-msg', 'dict-item', 'receive_status.21', '已刪除', true),
    ('c1be32f1-2d9e-4564-8778-328cf2d57c80', 'en_US', 'kudos-msg', 'dict-item', 'receive_status.21', 'Deleted', true);

--endregion DML
