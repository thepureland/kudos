--region DDL
-- Role-grant approval request: a pending workflow record for assigning a role to a user.
--
-- Lifecycle: PENDING → APPROVED | REJECTED | CANCELLED.
--   - submit  : requester creates a PENDING row (one per (role,user) that isn't already pending).
--   - approve : approver flips PENDING → APPROVED and the service performs the actual bind
--               (reusing AuthRoleUserService.batchBind, so SoD checks still apply).
--   - reject  : approver flips PENDING → REJECTED (no bind).
--   - cancel  : requester flips their own PENDING → CANCELLED.
--
-- Terminal states (APPROVED/REJECTED/CANCELLED) are immutable; re-requesting needs a new row.
create table if not exists "auth_role_grant_request"
(
    "id"               character(36)          default RANDOM_UUID() not null primary key,
    "role_id"          character varying(36)  not null,
    "user_id"          character varying(36)  not null,
    "tenant_id"        character varying(36)  not null,
    "status"           character varying(16)  not null,
    "reason"           character varying(512),
    "requester_id"     character varying(36),
    "request_time"     timestamp(6),
    "approver_id"      character varying(36),
    "decision_comment" character varying(512),
    "decision_time"    timestamp(6),
    "create_user_id"   character varying(36),
    "create_user_name" character varying(32),
    "create_time"      timestamp(6),
    "update_user_id"   character varying(36),
    "update_user_name" character varying(32),
    "update_time"      timestamp(6)
);

comment on table  "auth_role_grant_request" is '角色授予审批请求';
comment on column "auth_role_grant_request"."id"               is '主键';
comment on column "auth_role_grant_request"."role_id"          is '申请授予的角色ID';
comment on column "auth_role_grant_request"."user_id"          is '被授予的用户ID';
comment on column "auth_role_grant_request"."tenant_id"        is '租户ID';
comment on column "auth_role_grant_request"."status"           is '状态：PENDING/APPROVED/REJECTED/CANCELLED';
comment on column "auth_role_grant_request"."reason"           is '申请理由';
comment on column "auth_role_grant_request"."requester_id"     is '申请人ID';
comment on column "auth_role_grant_request"."request_time"     is '申请时间';
comment on column "auth_role_grant_request"."approver_id"      is '审批人ID';
comment on column "auth_role_grant_request"."decision_comment" is '审批意见';
comment on column "auth_role_grant_request"."decision_time"    is '审批时间';

-- Approver dashboard query is "PENDING within tenant, newest first" — index the hot columns.
create index if not exists "idx_auth_role_grant_request_status" on "auth_role_grant_request" ("status");
create index if not exists "idx_auth_role_grant_request_tenant" on "auth_role_grant_request" ("tenant_id");
create index if not exists "idx_auth_role_grant_request_user"   on "auth_role_grant_request" ("user_id");
-- Partial uniqueness (one open request per role+user) is enforced at the service layer because
-- H2/Postgres partial unique indexes differ in syntax; a full unique would wrongly block a second
-- request after the first is rejected.
create index if not exists "idx_auth_role_grant_request_role_user" on "auth_role_grant_request" ("role_id", "user_id");
--endregion DDL


--region DML
-- No seed data.
--endregion DML
