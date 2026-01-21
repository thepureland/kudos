--region DDL
create table if not exists "sys_dict"
(
    "id"          character(36) default RANDOM_UUID() not null primary key,
    "dict_type"   character varying(64)               not null,
    "dict_name"   character varying(64)               not null,
    "module_code" character varying(32)               not null,
    "remark"      character varying(300),
    "active"      boolean       default TRUE          not null,
    "built_in"    boolean       default FALSE         not null,
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp     default now(),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp
);

create unique index if not exists "uq_sys_dict" on "sys_dict" ("dict_type", "module_code");

comment on table "sys_dict" is '字典';
comment on column "sys_dict"."id" is '主键';
comment on column "sys_dict"."dict_type" is '字典类型';
comment on column "sys_dict"."dict_name" is '字典名称';
comment on column "sys_dict"."module_code" is '模块编码';
comment on column "sys_dict"."remark" is '备注';
comment on column "sys_dict"."active" is '是否启用';
comment on column "sys_dict"."built_in" is '是否内置';
comment on column "sys_dict"."create_user_id" is '创建者id';
comment on column "sys_dict"."create_user_name" is '创建者名称';
comment on column "sys_dict"."create_time" is '创建时间';
comment on column "sys_dict"."update_user_id" is '更新者id';
comment on column "sys_dict"."update_user_name" is '更新者名称';
comment on column "sys_dict"."update_time" is '更新时间';
--endregion DDL


--region DML
merge into "sys_dict" ("id", "module_code", "dict_type", "dict_name", "remark", "active", "built_in")
    values ('68139ed2-dbce-47fa-ac0d-2932fb0ee5ad', 'kudos-sys', 'ds_use', '数据源用途', null, true, false),
           ('d9f17338-8751-4d3b-bdd1-91a1b6f42432', 'kudos-sys', 'ds_type', '数据源类型',
            '暂时只支持一种数据源类型hikariCP', true, false),
           ('339b4cf1-6af4-49db-be1c-ee606959a689', 'kudos-sys', 'resource_type', '资源类型', null, true, false),
           ('3e90198e-332c-4622-bd9c-053fc8a4bcd3', 'kudos-user', 'dept_type', '组织类型', '部门类型', true, true),
           ('4a926e98-b6c0-437f-b474-ce06ea7b4bdb', 'kudos-user', 'sex', '性别', null, true, true),
           ('f9bbf04b-46d2-4aa2-bc93-dd3a5210de7b', 'kudos-sys', 'operate_type', '操作类型', null, true, true),
           ('2601c57f-3900-4be8-9ebf-e79781db9d3d', 'kudos-sys', 'cache_strategy', '缓存策略', null, true, false),
           ('69b23e60-57e4-40e3-9a5e-de2d71388b2d', 'workflow_edit', 'category', '流程分类', '工作流程定义分类', true,
            false),
           ('634c261d-78a3-4647-be09-64204c38b7d5', 'workflow_form_edit', 'category', '表单分类', '表单定义分类', true,
            false),
           ('54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'kudos-sys', 'language', '语言', '国际化语言', true, true),
           ('7dd94c04-4db0-4fae-bebc-d587e4940b67', 'kudos-notice', 'contact_way_status', '联系方式状态', null, true,
            true),
           ('895250ec-8d5c-454f-9d3e-b7b2ee2095bb', 'kudos-notice', 'contact_way_type', '联系方式类型', null, true,
            true),
           ('b2980de6-7fcb-4d42-a63e-e5a760eefd46', 'kudos-notice', 'email_interface_status', '邮件接口状态', null,
            true, true),
           ('a4288320-897b-4c72-abaf-260d75714158', 'kudos-notice', 'sms_interface_status', '短信接口状态', null, true,
            true),
           ('181c57ec-df00-4844-a79b-5b1019ec25ec', 'kudos-notice', 'publish_method', '通知发布方式', null, true, true),
           ('0739cfe7-1c78-45c1-a50a-91f415c66c7c', 'kudos-notice', 'receiver_group_type', '通知接收群组', null, true,
            true),
           ('1c147a5b-0543-497d-bcae-221aec84256c', 'kudos-notice', 'send_status', '通知发送状态', null, true, true),
           ('331dd7f9-77b7-49af-87d0-a1d7046bfb20', 'kudos-notice', 'tmpl_type', '通知模板类型', null, true, true),
           ('27dcee56-af0c-46c3-b717-f2f3e61f4c84', 'kudos-notice', 'auto_event_type', '系统通知模板事件类型', null,
            true, true),
           ('3ff26ac3-6c1c-4334-a83b-d8042ccf3b8c', 'kudos-notice', 'manual_event_type', '手动通知模板事件类型', null,
            true, true),
           ('0e52709f-93db-4495-a9af-48483982bc32', 'kudos-notice', 'sms_app_type', '短信平台类型', null, true, true),
           ('650aaa6b-739a-443f-8689-5260b5238a93', 'kudos-notice', 'sms_aliyun_region', '阿里云短信接口区域',
            '阿里云短信支持的国家/地区', true, true),
           ('2eab7ad1-dfb8-4d73-b6b2-37cfd14d225b', 'kudos-notice', 'sms_aws_region', '亚马逊短信接口区域',
            '亚马逊短信支持的国家/地区', true, true),
           ('9a70bb50-b330-42f9-9658-f8d8cd5b1679', 'kudos-notice', 'params', '通知参数', null, true, true),
           ('d3fd4de0-f32e-4fde-9a23-78b2f2705a7c', 'sys_captcha', 'LoadTypeEnum', '验证码资源加载方式', null, true,
            true),
           ('8cc7a3a9-906d-41ec-a17d-645409a73ba3', 'sys_captcha', 'CaptchaTrackTypeEnum', '验证码跟踪类型', null, true,
            true),
           ('d33fac57-cb42-4aa6-84c2-101ada37498f', 'sys_captcha', 'CaptchaTypeEnum', '图形验证码类型', null, true,
            true),
           ('d275942e-262b-460a-917e-ec96aab565cc', 'kudos-notice', 'receive_status', '消息接收状态', null, true, true),
           ('1b87ef01-c033-06a6-0525-b317b623899f', 'kudos-sys', 'timezone', '时区', null, true, false),
           ('e960b247-16e0-4f4e-a767-2b17eb5b6982', 'kudos-sys', 'domain_type', '域名类型', null, true, true),
           ('ad52c551-01c1-4c7f-9a96-720eecb32885', 'kudos-sys', 'terminal_type', '终端类型', null, true, true),
           ('ad52c541-02c1-3c7f-1a96-a20eecb32881', 'kudos-sys', 'access_rule_type', '访问规则类型', null, true, true);
--endregion DML


