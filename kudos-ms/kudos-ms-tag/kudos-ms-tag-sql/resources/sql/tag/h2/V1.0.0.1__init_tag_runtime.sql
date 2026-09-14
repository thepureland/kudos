create table tag_subject (
    tenant_id varchar(64) not null,
    subject_type varchar(128) not null,
    subject_id varchar(128) not null,
    display_name varchar(256),
    profile_json text,
    state_version bigint not null,
    first_seen_time timestamp(6) not null,
    update_time timestamp(6) not null,
    primary key (tenant_id, subject_type, subject_id),
    constraint fk_tag_subject_type foreign key (subject_type) references tag_subject_type (code),
    constraint ck_tag_subject_version check (state_version >= 0)
);

create table tag_attribute_event (
    event_id char(36) primary key,
    request_id varchar(128),
    payload_checksum varchar(64) not null,
    tenant_id varchar(64) not null,
    subject_type varchar(128) not null,
    subject_id varchar(128) not null,
    attribute_id char(36) not null,
    operation enum('SET', 'ADD', 'APPEND', 'REMOVE', 'CLEAR') not null,
    source_code varchar(128) not null,
    source_version bigint,
    occurred_time timestamp(6) not null,
    received_time timestamp(6) not null,
    process_status enum('RECEIVED', 'APPLIED', 'DUPLICATE', 'REJECTED', 'RETRYABLE_FAILED') not null,
    error_code varchar(64),
    error_message varchar(1000),
    value_type enum('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATE', 'DATETIME'),
    string_value varchar(2000),
    integer_value bigint,
    decimal_value decimal(38, 12),
    boolean_value boolean,
    date_value date,
    datetime_value timestamp(6),
    constraint fk_tag_attribute_event_attribute foreign key (attribute_id) references tag_attribute_definition (id),
    constraint fk_tag_attribute_event_subject foreign key (tenant_id, subject_type, subject_id) references tag_subject (tenant_id, subject_type, subject_id),
    constraint ck_tag_attribute_event_source_version check (source_version is null or source_version >= 0)
);

create index idx_tag_attribute_event_subject on tag_attribute_event (tenant_id, subject_type, subject_id, received_time);

create table tag_attribute_state (
    id char(36) primary key,
    tenant_id varchar(64) not null,
    subject_type varchar(128) not null,
    subject_id varchar(128) not null,
    attribute_id char(36) not null,
    value_key varchar(64) not null,
    value_type enum('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATE', 'DATETIME') not null,
    string_value varchar(2000),
    integer_value bigint,
    decimal_value decimal(38, 12),
    boolean_value boolean,
    date_value date,
    datetime_value timestamp(6),
    source_event_id char(36) not null,
    source_version bigint,
    state_version bigint not null,
    effective_time timestamp(6) not null,
    expire_time timestamp(6),
    update_time timestamp(6) not null,
    constraint uk_tag_attribute_state_value unique (tenant_id, subject_type, subject_id, attribute_id, value_key),
    constraint fk_tag_attribute_state_subject foreign key (tenant_id, subject_type, subject_id) references tag_subject (tenant_id, subject_type, subject_id),
    constraint fk_tag_attribute_state_attribute foreign key (attribute_id) references tag_attribute_definition (id),
    constraint fk_tag_attribute_state_event foreign key (source_event_id) references tag_attribute_event (event_id),
    constraint ck_tag_attribute_state_versions check ((source_version is null or source_version >= 0) and state_version >= 0),
    constraint ck_tag_attribute_state_typed_value check (
        (value_type = 'STRING' and string_value is not null and integer_value is null and decimal_value is null and boolean_value is null and date_value is null and datetime_value is null)
        or (value_type = 'INTEGER' and string_value is null and integer_value is not null and decimal_value is null and boolean_value is null and date_value is null and datetime_value is null)
        or (value_type = 'DECIMAL' and string_value is null and integer_value is null and decimal_value is not null and boolean_value is null and date_value is null and datetime_value is null)
        or (value_type = 'BOOLEAN' and string_value is null and integer_value is null and decimal_value is null and boolean_value is not null and date_value is null and datetime_value is null)
        or (value_type = 'DATE' and string_value is null and integer_value is null and decimal_value is null and boolean_value is null and date_value is not null and datetime_value is null)
        or (value_type = 'DATETIME' and string_value is null and integer_value is null and decimal_value is null and boolean_value is null and date_value is null and datetime_value is not null)
    )
);

create index idx_tag_attribute_state_subject on tag_attribute_state (tenant_id, subject_type, attribute_id, subject_id);
create index idx_tag_attribute_state_string on tag_attribute_state (tenant_id, subject_type, attribute_id, string_value, subject_id);
create index idx_tag_attribute_state_integer on tag_attribute_state (tenant_id, subject_type, attribute_id, integer_value, subject_id);
create index idx_tag_attribute_state_decimal on tag_attribute_state (tenant_id, subject_type, attribute_id, decimal_value, subject_id);
create index idx_tag_attribute_state_boolean on tag_attribute_state (tenant_id, subject_type, attribute_id, boolean_value, subject_id);
create index idx_tag_attribute_state_date on tag_attribute_state (tenant_id, subject_type, attribute_id, date_value, subject_id);
create index idx_tag_attribute_state_datetime on tag_attribute_state (tenant_id, subject_type, attribute_id, datetime_value, subject_id);

create table tag_membership (
    id char(36) primary key,
    tenant_id varchar(64) not null,
    subject_type varchar(128) not null,
    subject_id varchar(128) not null,
    tag_id char(36) not null,
    source_type enum('RULE', 'MANUAL', 'IMPORT', 'DEFAULT') not null,
    source_ref varchar(128) not null,
    active boolean not null,
    rule_version bigint,
    effective_from timestamp(6),
    effective_until timestamp(6),
    membership_version bigint not null,
    source_event_id char(36),
    update_time timestamp(6) not null,
    constraint uk_tag_membership_source unique (tenant_id, subject_type, subject_id, tag_id, source_type, source_ref),
    constraint fk_tag_membership_subject foreign key (tenant_id, subject_type, subject_id) references tag_subject (tenant_id, subject_type, subject_id),
    constraint fk_tag_membership_tag foreign key (tag_id) references tag_definition (id),
    constraint fk_tag_membership_event foreign key (source_event_id) references tag_attribute_event (event_id),
    constraint ck_tag_membership_versions check ((rule_version is null or rule_version > 0) and membership_version >= 0),
    constraint ck_tag_membership_effective check (effective_until is null or effective_from is null or effective_until > effective_from)
);

create index idx_tag_membership_lookup on tag_membership (tenant_id, subject_type, tag_id, active, subject_id);

create table tag_assignment (
    tenant_id varchar(64) not null,
    subject_type varchar(128) not null,
    subject_id varchar(128) not null,
    tag_id char(36) not null,
    exclusive_set_id char(36),
    assignment_version bigint not null,
    evaluated_rule_version bigint,
    materialized_time timestamp(6) not null,
    effective_from timestamp(6),
    effective_until timestamp(6),
    update_time timestamp(6) not null,
    primary key (tenant_id, subject_type, subject_id, tag_id),
    constraint uk_tag_assignment_exclusive unique (tenant_id, subject_type, subject_id, exclusive_set_id),
    constraint fk_tag_assignment_subject foreign key (tenant_id, subject_type, subject_id) references tag_subject (tenant_id, subject_type, subject_id),
    constraint fk_tag_assignment_tag foreign key (tag_id) references tag_definition (id),
    constraint fk_tag_assignment_set foreign key (exclusive_set_id) references tag_set (id),
    constraint ck_tag_assignment_versions check (assignment_version >= 0 and (evaluated_rule_version is null or evaluated_rule_version > 0)),
    constraint ck_tag_assignment_effective check (effective_until is null or effective_from is null or effective_until > effective_from)
);

create index idx_tag_assignment_lookup on tag_assignment (tenant_id, subject_type, tag_id, subject_id);

create table tag_assignment_event (
    event_id char(36) primary key,
    tenant_id varchar(64) not null,
    subject_type varchar(128) not null,
    subject_id varchar(128) not null,
    tag_id char(36) not null,
    operation enum('ASSIGNED', 'REMOVED') not null,
    cause_type varchar(32) not null,
    cause_ref varchar(128),
    assignment_version bigint not null,
    occurred_time timestamp(6) not null,
    constraint fk_tag_assignment_event_subject foreign key (tenant_id, subject_type, subject_id) references tag_subject (tenant_id, subject_type, subject_id),
    constraint fk_tag_assignment_event_tag foreign key (tag_id) references tag_definition (id),
    constraint ck_tag_assignment_event_version check (assignment_version >= 0)
);

create table tag_manual_assignment_event (
    event_id char(36) primary key,
    payload_checksum char(64) not null,
    request_id varchar(128),
    tenant_id varchar(64) not null,
    subject_type varchar(128) not null,
    subject_id varchar(128) not null,
    tag_id char(36) not null,
    operation enum('ASSIGN', 'REMOVE') not null,
    manual_source_ref varchar(128),
    operator_id varchar(128) not null,
    operator_name varchar(128),
    reason varchar(1000),
    effective_until timestamp(6),
    occurred_time timestamp(6) not null,
    received_time timestamp(6) not null,
    process_status enum('RECEIVED', 'APPLIED') not null,
    constraint fk_tag_manual_event_subject foreign key (tenant_id, subject_type, subject_id) references tag_subject (tenant_id, subject_type, subject_id),
    constraint fk_tag_manual_event_tag foreign key (tag_id) references tag_definition (id)
);

create table tag_recalculation_job (
    id char(36) primary key,
    job_key varchar(512) not null,
    tenant_id varchar(64) not null,
    job_type enum('SUBJECT_INCREMENTAL', 'RULE_FULL_REBUILD') not null,
    tag_id char(36) not null,
    rule_version bigint not null,
    subject_type varchar(128) not null,
    subject_id varchar(128),
    cursor_subject_id varchar(128),
    status enum('PENDING', 'RUNNING', 'SUCCEEDED', 'RETRY_WAIT', 'FAILED', 'CANCELLED') not null,
    priority integer not null,
    requested_version bigint not null,
    processed_version bigint not null,
    attempt_count integer not null,
    max_attempts integer not null,
    available_time timestamp(6) not null,
    lease_owner varchar(128),
    lease_until timestamp(6),
    processed_count bigint not null,
    last_error_code varchar(64),
    last_error_message varchar(1000),
    create_time timestamp(6) not null,
    start_time timestamp(6),
    complete_time timestamp(6),
    update_time timestamp(6) not null,
    version bigint not null,
    constraint uk_tag_recalculation_job_key unique (job_key),
    constraint fk_tag_recalculation_job_tag foreign key (tag_id) references tag_definition (id),
    constraint ck_tag_recalculation_job_priority check (priority >= 0),
    constraint ck_tag_recalculation_job_versions check (requested_version >= processed_version and processed_version >= 0 and rule_version > 0 and version >= 0),
    constraint ck_tag_recalculation_job_attempts check (attempt_count >= 0 and max_attempts > 0 and attempt_count <= max_attempts),
    constraint ck_tag_recalculation_job_processed check (processed_count >= 0)
);

create index idx_tag_recalculation_job_available on tag_recalculation_job (status, available_time, priority, id);
create index idx_tag_recalculation_job_lease on tag_recalculation_job (lease_until, status);

create table tag_recalculation_candidate (
    run_id char(36) not null,
    tenant_id varchar(64) not null,
    subject_type varchar(128) not null,
    subject_id varchar(128) not null,
    tag_id char(36) not null,
    rule_version bigint not null,
    evaluated_time timestamp(6) not null,
    primary key (run_id, subject_type, subject_id, tag_id),
    constraint fk_tag_recalculation_candidate_run foreign key (run_id) references tag_recalculation_job (id) on delete cascade,
    constraint fk_tag_recalculation_candidate_tag foreign key (tag_id) references tag_definition (id),
    constraint ck_tag_recalculation_candidate_version check (rule_version > 0)
);

create index idx_tag_recalculation_candidate_lookup on tag_recalculation_candidate (run_id, subject_type, subject_id, tag_id);
