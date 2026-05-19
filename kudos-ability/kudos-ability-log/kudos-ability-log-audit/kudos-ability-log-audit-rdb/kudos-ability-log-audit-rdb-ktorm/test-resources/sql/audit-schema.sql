-- 与 rdb-common 模块 V20260519__create_sys_audit_log.sql 一致的 DDL，
-- 复制到测试侧以走 baomidou dynamic-datasource 的 init.schema 加载路径。
-- 列名 / 类型必须与 AuditLogSchema 单一真相源保持同步。

DROP TABLE IF EXISTS sys_audit_log;
CREATE TABLE sys_audit_log (
    id                    VARCHAR(64)   NOT NULL,
    entity_id             VARCHAR(64),
    operate_type_id       INT,
    operate_type          VARCHAR(64),
    module_id             INT,
    module_name           VARCHAR(255),
    module_code           VARCHAR(64),
    description           VARCHAR(1000),
    operator              VARCHAR(255),
    operator_id           VARCHAR(64),
    operator_user_type    VARCHAR(64),
    tenant_id             VARCHAR(64),
    source_tenant_id      VARCHAR(64),
    sub_sys_code          VARCHAR(64),
    operate_time          TIMESTAMP,
    operate_ip            BIGINT,
    operate_ip_dict_code  VARCHAR(64),
    client_os             VARCHAR(128),
    client_browser        VARCHAR(128),
    request_type          VARCHAR(16),
    PRIMARY KEY (id)
);

DROP TABLE IF EXISTS sys_audit_detail_log;
CREATE TABLE sys_audit_detail_log (
    id                  VARCHAR(64)   NOT NULL,
    audit_id            VARCHAR(64)   NOT NULL,
    operate_url         VARCHAR(512),
    string_params       TEXT,
    object_params       TEXT,
    request_referer     VARCHAR(512),
    request_form_data   TEXT,
    description         VARCHAR(2000),
    PRIMARY KEY (id)
);
