--region DML

-- dict
merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('a1a673fe-91e1-4990-b0bb-c8ad44e4254a', 'zh_CN', 'kudos-user', 'dict', 'account_type', '账号类型', true),
    ('20c8b9ce-6051-456e-a880-8807526244f9', 'zh_TW', 'kudos-user', 'dict', 'account_type', '帳號類型', true),
    ('2f81a042-23d6-47ac-9b76-fa29a24ce652', 'en_US', 'kudos-user', 'dict', 'account_type', 'Account Type', true),

    ('28ca4fb9-b9ee-476a-94ad-92e80200de76', 'zh_CN', 'kudos-user', 'dict', 'account_status', '账号状态', true),
    ('21d5d671-558f-473d-8d70-a78bfbdc991f', 'zh_TW', 'kudos-user', 'dict', 'account_status', '帳號狀態', true),
    ('78ce5aee-b63a-4a4c-9799-e45f512fc379', 'en_US', 'kudos-user', 'dict', 'account_status', 'Account Status', true),

    ('028b9ca9-7ef1-4fb0-a1a8-ea11529fd0a1', 'zh_CN', 'kudos-user', 'dict', 'account_provider', '第三方账号提供商', true),
    ('65fd80d6-31b7-4552-81ac-0dfcb445c687', 'zh_TW', 'kudos-user', 'dict', 'account_provider', '第三方帳號提供商', true),
    ('639ab7db-a9b3-4ab3-a75c-72e97ab57b4b', 'en_US', 'kudos-user', 'dict', 'account_provider', 'Third-party Account Provider', true),

    ('2542e929-9cb1-4344-9d88-3e57ab80a04b', 'zh_CN', 'kudos-user', 'dict', 'org_type', '机构类型', true),
    ('3136c960-8aa5-4b4e-b003-b33f599d2db0', 'zh_TW', 'kudos-user', 'dict', 'org_type', '機構類型', true),
    ('76e7569d-af9c-479f-a09f-b66f66c2b493', 'en_US', 'kudos-user', 'dict', 'org_type', 'Organization Type', true),

    ('04fa5f1c-230d-4a56-a159-7a120253150b', 'zh_CN', 'kudos-user', 'dict', 'contact_way', '联系方式', true),
    ('5ea5c856-e104-4c7e-b98c-e0fcec3aea4a', 'zh_TW', 'kudos-user', 'dict', 'contact_way', '聯絡方式', true),
    ('3c9c5bd7-b9df-45c1-afea-af8270b9e613', 'en_US', 'kudos-user', 'dict', 'contact_way', 'Contact Method', true),

    ('660b95a0-775e-4495-a6ab-8eb95871e104', 'zh_CN', 'kudos-user', 'dict', 'contact_way_status', '联系方式状态', true),
    ('383f5bd3-da84-42ee-bbd7-119364ba5680', 'zh_TW', 'kudos-user', 'dict', 'contact_way_status', '聯絡方式狀態', true),
    ('0d316a29-95ea-4068-bec7-e5259193b674', 'en_US', 'kudos-user', 'dict', 'contact_way_status', 'Contact Method Status', true);


-- dict-item account_type
merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('c3cd2ca1-80d4-4fb6-8fc0-bd930c3f692b', 'zh_CN', 'kudos-user', 'dict-item', 'account_type.00', '终端用户', true),
    ('f3f0052c-a3bc-4e5b-ad26-c1acd95841ad', 'zh_TW', 'kudos-user', 'dict-item', 'account_type.00', '終端使用者', true),
    ('4818f158-5bf6-49e4-8922-fa61338f7b0f', 'en_US', 'kudos-user', 'dict-item', 'account_type.00', 'End User', true),

    ('80093c47-dfcc-4369-9469-b5c4d4f683f2', 'zh_CN', 'kudos-user', 'dict-item', 'account_type.11', '租户管理员', true),
    ('74a8625e-759d-417c-8181-5c86ef3eb500', 'zh_TW', 'kudos-user', 'dict-item', 'account_type.11', '租戶管理員', true),
    ('482d1bda-b051-4b12-a428-54512d00d506', 'en_US', 'kudos-user', 'dict-item', 'account_type.11', 'Tenant Administrator', true),

    ('a440b931-9fd0-4303-9de1-1b55e48473c6', 'zh_CN', 'kudos-user', 'dict-item', 'account_type.12', '租户成员', true),
    ('394b4084-555a-4af6-8b72-4ae250b9fe01', 'zh_TW', 'kudos-user', 'dict-item', 'account_type.12', '租戶成員', true),
    ('db83d923-6df1-4514-8803-e0a3d412c9e7', 'en_US', 'kudos-user', 'dict-item', 'account_type.12', 'Tenant Member', true),

    ('68196b88-34f1-40f1-a862-3e23df49ab98', 'zh_CN', 'kudos-user', 'dict-item', 'account_type.21', '门户管理员', true),
    ('365a86e1-3046-4457-8d9c-efbc43329019', 'zh_TW', 'kudos-user', 'dict-item', 'account_type.21', '門戶管理員', true),
    ('419232f9-f249-4d9a-8abe-9499295d355d', 'en_US', 'kudos-user', 'dict-item', 'account_type.21', 'Portal Administrator', true),

    ('0d7f661d-ef63-449b-9613-f460a1e4e07e', 'zh_CN', 'kudos-user', 'dict-item', 'account_type.22', '门户成员', true),
    ('175cb13b-6828-4277-8483-429ee84a7a34', 'zh_TW', 'kudos-user', 'dict-item', 'account_type.22', '門戶成員', true),
    ('699eeb6b-de19-423f-ae9c-1c62e4801e5b', 'en_US', 'kudos-user', 'dict-item', 'account_type.22', 'Portal Member', true);

-- dict-item account_status
merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('665fb756-ca21-478b-8764-c6d6368fb6a2', 'zh_CN', 'kudos-user', 'dict-item', 'account_status.00', '已注销', true),
    ('07c6d0e5-eb7e-4579-8232-345432b74ea4', 'zh_TW', 'kudos-user', 'dict-item', 'account_status.00', '已註銷', true),
    ('93c21c47-bccc-41d1-8c75-9cca3f9081c1', 'en_US', 'kudos-user', 'dict-item', 'account_status.00', 'Deactivated', true),

    ('b25a528f-3d79-4c83-94c2-4e297558568c', 'zh_CN', 'kudos-user', 'dict-item', 'account_status.10', '正常', true),
    ('6969313e-dc64-4902-a21e-20d8ac67ab12', 'zh_TW', 'kudos-user', 'dict-item', 'account_status.10', '正常', true),
    ('03e0abf8-07c7-4487-b891-e6534e01237c', 'en_US', 'kudos-user', 'dict-item', 'account_status.10', 'Normal', true),

    ('5ab61574-645b-42f7-b676-6dcde044674e', 'zh_CN', 'kudos-user', 'dict-item', 'account_status.20', '锁定', true),
    ('4e5756cd-fa2a-4121-99ad-118e8c1cd48e', 'zh_TW', 'kudos-user', 'dict-item', 'account_status.20', '鎖定', true),
    ('aa232471-9fe8-4588-8695-babd14f23fed', 'en_US', 'kudos-user', 'dict-item', 'account_status.20', 'Locked', true),

    ('8e71acc4-13ea-48be-b339-bc91a94b78f9', 'zh_CN', 'kudos-user', 'dict-item', 'account_status.30', '账号过期', true),
    ('ec135061-04ab-4716-888d-efadf91c1d34', 'zh_TW', 'kudos-user', 'dict-item', 'account_status.30', '帳號過期', true),
    ('feae9b9f-4b09-48f8-871e-28e6a8804bad', 'en_US', 'kudos-user', 'dict-item', 'account_status.30', 'Account Expired', true),

    ('bc679d2b-d392-4b13-9d86-c920585660bf', 'zh_CN', 'kudos-user', 'dict-item', 'account_status.40', '凭证过期', true),
    ('48158071-b3e6-4a29-8caf-f23872cee424', 'zh_TW', 'kudos-user', 'dict-item', 'account_status.40', '憑證過期', true),
    ('04adcbf3-860a-4ffe-9600-bf01e94e2141', 'en_US', 'kudos-user', 'dict-item', 'account_status.40', 'Credential Expired', true);

-- dict-item account_provider
merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('eb2a0d59-8774-45a4-952f-88cee7363b7a', 'zh_CN', 'kudos-user', 'dict-item', 'account_provider.google', '谷歌', true),
    ('6437cb8c-c87a-4ce3-9798-386002f0a656', 'zh_TW', 'kudos-user', 'dict-item', 'account_provider.google', 'Google', true),
    ('f44ea019-adbc-4a5b-b6d5-490c9d9d0d17', 'en_US', 'kudos-user', 'dict-item', 'account_provider.google', 'Google', true),

    ('f6ebc0cf-7be5-4bbd-8e12-467910fcb336', 'zh_CN', 'kudos-user', 'dict-item', 'account_provider.apple', '苹果', true),
    ('304eea70-f1aa-4282-a986-6a285d26806e', 'zh_TW', 'kudos-user', 'dict-item', 'account_provider.apple', 'Apple', true),
    ('1ea1aa9f-e3ff-4e03-9afb-80b287eba880', 'en_US', 'kudos-user', 'dict-item', 'account_provider.apple', 'Apple', true),

    ('8b6a345f-259c-4ed5-ac8f-db1125a43764', 'zh_CN', 'kudos-user', 'dict-item', 'account_provider.wechat', '微信', true),
    ('ff8637be-6a17-4840-ba06-b95572426e92', 'zh_TW', 'kudos-user', 'dict-item', 'account_provider.wechat', '微信', true),
    ('e95c4b21-a10f-4181-a4a6-8563c9ea93ad', 'en_US', 'kudos-user', 'dict-item', 'account_provider.wechat', 'WeChat', true),

    ('f828a38c-d145-42f2-b80c-0755c80328fe', 'zh_CN', 'kudos-user', 'dict-item', 'account_provider.github', 'github', true),
    ('78ab4c0d-c67b-4696-a6cd-a48a802b15d7', 'zh_TW', 'kudos-user', 'dict-item', 'account_provider.github', 'GitHub', true),
    ('e0f0239f-5f0a-43c1-a75b-0d28846ce5c2', 'en_US', 'kudos-user', 'dict-item', 'account_provider.github', 'GitHub', true);

-- dict-item org_type
merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('efc27798-b9d1-40cb-96ba-5b7f9307064a', 'zh_CN', 'kudos-user', 'dict-item', 'org_type.00', '总部', true),
    ('ea72cc4c-7ddc-4577-b9a8-c381315493fd', 'zh_TW', 'kudos-user', 'dict-item', 'org_type.00', '總部', true),
    ('63f208ec-3dca-441e-a3cc-dbbec8ef1a37', 'en_US', 'kudos-user', 'dict-item', 'org_type.00', 'Headquarters', true),

    ('333a5738-d2b8-4f85-a7b7-ecfe33108e6f', 'zh_CN', 'kudos-user', 'dict-item', 'org_type.10', '分公司', true),
    ('d4d151c5-bca7-44da-9812-e8259468e2ce', 'zh_TW', 'kudos-user', 'dict-item', 'org_type.10', '分公司', true),
    ('cbf83923-cdef-493e-9578-9d55c96d1d29', 'en_US', 'kudos-user', 'dict-item', 'org_type.10', 'Branch', true),

    ('ee1b37c1-a789-4d42-85a9-1285bddf1107', 'zh_CN', 'kudos-user', 'dict-item', 'org_type.20', '事业部', true),
    ('ae006e17-5c54-49b0-92d8-fb16ee0afcee', 'zh_TW', 'kudos-user', 'dict-item', 'org_type.20', '事業部', true),
    ('77ba8eda-3bfe-4064-a771-a636c0bf48ba', 'en_US', 'kudos-user', 'dict-item', 'org_type.20', 'Division', true),

    ('5865d681-b94f-460c-a1e7-c9eb491709d3', 'zh_CN', 'kudos-user', 'dict-item', 'org_type.30', '部门', true),
    ('1efd6235-fad1-4c9a-910c-e1acbc3b0f2f', 'zh_TW', 'kudos-user', 'dict-item', 'org_type.30', '部門', true),
    ('1550a791-bdd0-48bb-8c6c-03042a8d159b', 'en_US', 'kudos-user', 'dict-item', 'org_type.30', 'Department', true),

    ('bfa2153e-165d-4e02-8514-9569bfe3e024', 'zh_CN', 'kudos-user', 'dict-item', 'org_type.40', '小组', true),
    ('e75fb9d5-2224-442a-a797-40968ba5d94c', 'zh_TW', 'kudos-user', 'dict-item', 'org_type.40', '小組', true),
    ('6d9d315f-1105-4809-af3d-f91bc820ff87', 'en_US', 'kudos-user', 'dict-item', 'org_type.40', 'Team', true);

-- dict-item contact_way
merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('8050e30f-65f5-4ff4-8c4f-a584b304202f', 'zh_CN', 'kudos-user', 'dict-item', 'contact_way.101', '手机', true),
    ('6f48dfc9-807e-4157-9d24-3e81b3019f0f', 'zh_TW', 'kudos-user', 'dict-item', 'contact_way.101', '手機', true),
    ('1fe9aa13-99fe-40a4-b93f-19ff3ef3c0cb', 'en_US', 'kudos-user', 'dict-item', 'contact_way.101', 'Mobile Phone', true),

    ('3ff5ba2a-d989-4c68-8413-eea0052f39a3', 'zh_CN', 'kudos-user', 'dict-item', 'contact_way.102', '固定电话', true),
    ('77ba9770-b5d9-4389-ae5b-1ee6a4b94f3b', 'zh_TW', 'kudos-user', 'dict-item', 'contact_way.102', '固定電話', true),
    ('ad9bcc57-4905-4da1-83d4-9f3f28c94d5c', 'en_US', 'kudos-user', 'dict-item', 'contact_way.102', 'Landline', true),

    ('e0f73749-2377-45dd-a229-281913e70fea', 'zh_CN', 'kudos-user', 'dict-item', 'contact_way.201', 'email', true),
    ('819eaada-ba6d-4f51-a237-d29076f22183', 'zh_TW', 'kudos-user', 'dict-item', 'contact_way.201', '電子郵件', true),
    ('5dfdee3e-b7cd-4f66-9745-f8efc91119a9', 'en_US', 'kudos-user', 'dict-item', 'contact_way.201', 'Email', true),

    ('7cd9bf37-b3ab-4947-a8c0-d2372995d3ef', 'zh_CN', 'kudos-user', 'dict-item', 'contact_way.301', 'whatsapp', true),
    ('223eb599-bda6-43c8-b9eb-c09cbb5ead24', 'zh_TW', 'kudos-user', 'dict-item', 'contact_way.301', 'WhatsApp', true),
    ('6ff71d6d-915f-4195-8105-f2f68aa5d135', 'en_US', 'kudos-user', 'dict-item', 'contact_way.301', 'WhatsApp', true),

    ('7c0e7893-e9ed-463d-8706-8a388896883a', 'zh_CN', 'kudos-user', 'dict-item', 'contact_way.302', 'wechat', true),
    ('05d1b86e-efa5-4a19-8077-356317161148', 'zh_TW', 'kudos-user', 'dict-item', 'contact_way.302', 'WeChat', true),
    ('039b5a4b-d5f7-41bd-835b-2fe461650190', 'en_US', 'kudos-user', 'dict-item', 'contact_way.302', 'WeChat', true),

    ('f22f53f6-a20d-41b7-a199-f8113c3e8c2a', 'zh_CN', 'kudos-user', 'dict-item', 'contact_way.401', 'douyin', true),
    ('90d24455-d191-4713-ad6e-a3a69d241d53', 'zh_TW', 'kudos-user', 'dict-item', 'contact_way.401', '抖音', true),
    ('a641f44b-6f46-4381-b5db-9e30d34ee9d7', 'en_US', 'kudos-user', 'dict-item', 'contact_way.401', 'Douyin', true),

    ('75719ab6-769a-40b8-b36c-0ae331080169', 'zh_CN', 'kudos-user', 'dict-item', 'contact_way.402', 'tiktok', true),
    ('4061d9b8-71c7-4397-886d-621ad276aef4', 'zh_TW', 'kudos-user', 'dict-item', 'contact_way.402', 'TikTok', true),
    ('24b83627-35b1-4361-bc77-6857430f3b59', 'en_US', 'kudos-user', 'dict-item', 'contact_way.402', 'TikTok', true),

    ('e6e989ae-37c7-4afa-9514-82673ef18509', 'zh_CN', 'kudos-user', 'dict-item', 'contact_way.403', 'facebook', true),
    ('89cd1f6f-8433-41cc-be91-c37d68fc7894', 'zh_TW', 'kudos-user', 'dict-item', 'contact_way.403', 'Facebook', true),
    ('fd83b9f2-8225-4125-959f-62d0a16b00b8', 'en_US', 'kudos-user', 'dict-item', 'contact_way.403', 'Facebook', true);

-- dict-item contact_way_status
merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('5ce8795e-c0e6-414b-9906-0d9c5706b471', 'zh_CN', 'kudos-user', 'dict-item', 'contact_way_status.00', '未验证', true),
    ('7b6d552f-a9cd-4b0a-a899-aae7307a6d39', 'zh_TW', 'kudos-user', 'dict-item', 'contact_way_status.00', '未驗證', true),
    ('95451300-2cc4-4313-ab36-33b1294dd78d', 'en_US', 'kudos-user', 'dict-item', 'contact_way_status.00', 'Unverified', true),

    ('86351515-aa5d-4966-a48e-7b67e1edc45d', 'zh_CN', 'kudos-user', 'dict-item', 'contact_way_status.10', '正常', true),
    ('d35eec51-770d-487e-ac54-4e0b7673de80', 'zh_TW', 'kudos-user', 'dict-item', 'contact_way_status.10', '正常', true),
    ('df5b8090-a7ac-4b14-a84f-a6a0486b65f2', 'en_US', 'kudos-user', 'dict-item', 'contact_way_status.10', 'Normal', true),

    ('adea570a-1846-4817-990b-a948a9ea6d7d', 'zh_CN', 'kudos-user', 'dict-item', 'contact_way_status.20', '无法联系', true),
    ('cec16692-451e-4b53-aede-3d2f150e8ffd', 'zh_TW', 'kudos-user', 'dict-item', 'contact_way_status.20', '無法聯絡', true),
    ('934cbeda-c489-4554-8e5e-cba35cee3137', 'en_US', 'kudos-user', 'dict-item', 'contact_way_status.20', 'Unreachable', true),

    ('42da40f2-bb0e-46cc-8cac-e840bab54d61', 'zh_CN', 'kudos-user', 'dict-item', 'contact_way_status.30', '非本人联系方式', true),
    ('f2542f49-3112-4f61-8c7a-3a3476c2aeec', 'zh_TW', 'kudos-user', 'dict-item', 'contact_way_status.30', '非本人聯絡方式', true),
    ('ee696b9a-198a-44bf-b301-2ce6ab80f5ae', 'en_US', 'kudos-user', 'dict-item', 'contact_way_status.30', 'Not Owner''s Contact', true);

--endregion DML
