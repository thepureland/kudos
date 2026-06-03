-- region DDL

alter table `msg_send` add column `idempotency_key` varchar(64) comment '幂等键（业务请求唯一标识，重复发送去重用）' after `job_id`;

-- 同一租户下幂等键唯一；idempotency_key 为 NULL 的记录不参与唯一约束（允许多条 NULL）
create unique index `uq_msg_send__tenant_idempotency` on `msg_send` (`tenant_id`, `idempotency_key`);

-- endregion DDL


-- region DML

-- endregion DML
