-- Kudos audit log 主表 / 详情表 DDL。
-- 类型与长度按主流业务场景估算，超长字段（描述 / 参数 JSON）走 TEXT。
-- 实际部署时业务方可以基于本表加扩展列、分库分表策略，但**列名 / 主表名不要改**——
-- `kudos-ability-log-audit-rdb-common.AuditLogSchema` 是单一真相源。

CREATE TABLE IF NOT EXISTS sys_audit_log (
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

CREATE INDEX IF NOT EXISTS idx_sys_audit_log_tenant      ON sys_audit_log (tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_audit_log_operator    ON sys_audit_log (operator_id);
CREATE INDEX IF NOT EXISTS idx_sys_audit_log_module      ON sys_audit_log (module_code);
CREATE INDEX IF NOT EXISTS idx_sys_audit_log_operate_time ON sys_audit_log (operate_time);


CREATE TABLE IF NOT EXISTS sys_audit_detail_log (
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

CREATE INDEX IF NOT EXISTS idx_sys_audit_detail_log_audit_id ON sys_audit_detail_log (audit_id);
