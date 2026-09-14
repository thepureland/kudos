create table tag_subject_type (
    id char(36) primary key,
    code varchar(128) not null,
    name varchar(128) not null,
    owner_service_code varchar(128) not null,
    description varchar(1000),
    active boolean not null,
    built_in boolean not null,
    version bigint not null,
    create_user_id varchar(64),
    create_user_name varchar(128),
    create_time timestamp(6) not null,
    update_user_id varchar(64),
    update_user_name varchar(128),
    update_time timestamp(6) not null,
    constraint uk_tag_subject_type_code unique (code),
    constraint ck_tag_subject_type_version check (version >= 0)
);

create table tag_attribute_definition (
    id char(36) primary key,
    tenant_id varchar(64) not null,
    subject_type varchar(128) not null,
    code varchar(128) not null,
    name varchar(128) not null,
    description varchar(1000),
    value_type enum('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATE', 'DATETIME') not null,
    cardinality enum('SINGLE', 'MULTIPLE') not null,
    active boolean not null,
    built_in boolean not null,
    version bigint not null,
    create_user_id varchar(64),
    create_user_name varchar(128),
    create_time timestamp(6) not null,
    update_user_id varchar(64),
    update_user_name varchar(128),
    update_time timestamp(6) not null,
    constraint uk_tag_attribute_definition_code unique (tenant_id, subject_type, code),
    constraint fk_tag_attribute_definition_subject_type foreign key (subject_type) references tag_subject_type (code),
    constraint ck_tag_attribute_definition_version check (version >= 0)
);

create table tag_set (
    id char(36) primary key,
    tenant_id varchar(64) not null,
    subject_type varchar(128) not null,
    code varchar(128) not null,
    name varchar(128) not null,
    cardinality enum('SINGLE', 'MULTIPLE') not null,
    default_tag_id char(36),
    active boolean not null,
    built_in boolean not null,
    version bigint not null,
    create_user_id varchar(64),
    create_user_name varchar(128),
    create_time timestamp(6) not null,
    update_user_id varchar(64),
    update_user_name varchar(128),
    update_time timestamp(6) not null,
    constraint uk_tag_set_code unique (tenant_id, subject_type, code),
    constraint fk_tag_set_subject_type foreign key (subject_type) references tag_subject_type (code),
    constraint ck_tag_set_default check (default_tag_id is null or cardinality = 'SINGLE'),
    constraint ck_tag_set_version check (version >= 0)
);

create table tag_definition (
    id char(36) primary key,
    tenant_id varchar(64) not null,
    subject_type varchar(128) not null,
    code varchar(128) not null,
    name varchar(128) not null,
    description varchar(1000),
    tag_set_id char(36),
    set_priority integer not null,
    published_rule_id char(36),
    manual_assignable boolean not null,
    active boolean not null,
    built_in boolean not null,
    version bigint not null,
    create_user_id varchar(64),
    create_user_name varchar(128),
    create_time timestamp(6) not null,
    update_user_id varchar(64),
    update_user_name varchar(128),
    update_time timestamp(6) not null,
    constraint uk_tag_definition_code unique (tenant_id, subject_type, code),
    constraint fk_tag_definition_subject_type foreign key (subject_type) references tag_subject_type (code),
    constraint fk_tag_definition_set foreign key (tag_set_id) references tag_set (id),
    constraint ck_tag_definition_priority check (set_priority >= 0),
    constraint ck_tag_definition_version check (version >= 0)
);

create table tag_rule (
    id char(36) primary key,
    tenant_id varchar(64) not null,
    tag_id char(36) not null,
    rule_version bigint not null,
    status enum('DRAFT', 'REBUILDING', 'PUBLISHED', 'RETIRED') not null,
    root_node_id char(36),
    expression_version integer not null,
    checksum varchar(64) not null,
    published_time timestamp(6),
    retired_time timestamp(6),
    version bigint not null default 0,
    create_user_id varchar(64),
    create_user_name varchar(128),
    create_time timestamp(6) not null,
    update_user_id varchar(64),
    update_user_name varchar(128),
    update_time timestamp(6) not null,
    constraint uk_tag_rule_version unique (tag_id, rule_version),
    constraint fk_tag_rule_tag foreign key (tag_id) references tag_definition (id),
    constraint ck_tag_rule_versions check (rule_version > 0 and expression_version > 0 and version >= 0)
);

create table tag_rule_node (
    id char(36) primary key,
    rule_id char(36) not null,
    parent_id char(36),
    node_kind enum('ALL_OF', 'ANY_OF', 'NOT', 'ATTRIBUTE_PREDICATE', 'HAS_TAG') not null,
    order_num integer not null,
    attribute_id char(36),
    referenced_tag_id char(36),
    operator varchar(16),
    constraint fk_tag_rule_node_rule foreign key (rule_id) references tag_rule (id) on delete cascade,
    constraint fk_tag_rule_node_parent foreign key (parent_id) references tag_rule_node (id),
    constraint fk_tag_rule_node_attribute foreign key (attribute_id) references tag_attribute_definition (id),
    constraint fk_tag_rule_node_referenced_tag foreign key (referenced_tag_id) references tag_definition (id),
    constraint ck_tag_rule_node_order check (order_num >= 0)
);

create index idx_tag_rule_node_parent on tag_rule_node (rule_id, parent_id, order_num);

create table tag_rule_operand (
    id char(36) primary key,
    node_id char(36) not null,
    order_num integer not null,
    value_type enum('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATE', 'DATETIME') not null,
    string_value varchar(2000),
    integer_value bigint,
    decimal_value decimal(38, 12),
    boolean_value boolean,
    date_value date,
    datetime_value timestamp(6),
    constraint uk_tag_rule_operand_order unique (node_id, order_num),
    constraint fk_tag_rule_operand_node foreign key (node_id) references tag_rule_node (id) on delete cascade,
    constraint ck_tag_rule_operand_order check (order_num >= 0),
    constraint ck_tag_rule_operand_typed_value check (
        (value_type = 'STRING' and string_value is not null and integer_value is null and decimal_value is null and boolean_value is null and date_value is null and datetime_value is null)
        or (value_type = 'INTEGER' and string_value is null and integer_value is not null and decimal_value is null and boolean_value is null and date_value is null and datetime_value is null)
        or (value_type = 'DECIMAL' and string_value is null and integer_value is null and decimal_value is not null and boolean_value is null and date_value is null and datetime_value is null)
        or (value_type = 'BOOLEAN' and string_value is null and integer_value is null and decimal_value is null and boolean_value is not null and date_value is null and datetime_value is null)
        or (value_type = 'DATE' and string_value is null and integer_value is null and decimal_value is null and boolean_value is null and date_value is not null and datetime_value is null)
        or (value_type = 'DATETIME' and string_value is null and integer_value is null and decimal_value is null and boolean_value is null and date_value is null and datetime_value is not null)
    )
);

create table tag_rule_dependency (
    id char(36) primary key,
    tenant_id varchar(64) not null,
    rule_id char(36) not null,
    tag_id char(36) not null,
    dependency_type enum('ATTRIBUTE', 'TAG') not null,
    attribute_id char(36),
    referenced_tag_id char(36),
    constraint uk_tag_rule_dependency unique (rule_id, dependency_type, attribute_id, referenced_tag_id),
    constraint fk_tag_rule_dependency_rule foreign key (rule_id) references tag_rule (id) on delete cascade,
    constraint fk_tag_rule_dependency_tag foreign key (tag_id) references tag_definition (id),
    constraint fk_tag_rule_dependency_attribute foreign key (attribute_id) references tag_attribute_definition (id),
    constraint fk_tag_rule_dependency_referenced_tag foreign key (referenced_tag_id) references tag_definition (id),
    constraint ck_tag_rule_dependency_shape check (
        (dependency_type = 'ATTRIBUTE' and attribute_id is not null and referenced_tag_id is null)
        or (dependency_type = 'TAG' and attribute_id is null and referenced_tag_id is not null)
    )
);

create index idx_tag_rule_dependency_attribute on tag_rule_dependency (tenant_id, dependency_type, attribute_id, rule_id);
create index idx_tag_rule_dependency_tag on tag_rule_dependency (tenant_id, dependency_type, referenced_tag_id, rule_id);

create table tag_taxonomy_node (
    id char(36) primary key,
    tenant_id varchar(64) not null,
    subject_type varchar(128) not null,
    parent_id char(36),
    code varchar(128) not null,
    name varchar(128) not null,
    order_num integer not null,
    active boolean not null,
    built_in boolean not null,
    version bigint not null,
    create_user_id varchar(64),
    create_user_name varchar(128),
    create_time timestamp(6) not null,
    update_user_id varchar(64),
    update_user_name varchar(128),
    update_time timestamp(6) not null,
    constraint uk_tag_taxonomy_node_code unique (tenant_id, subject_type, code),
    constraint fk_tag_taxonomy_node_subject_type foreign key (subject_type) references tag_subject_type (code),
    constraint fk_tag_taxonomy_node_parent foreign key (parent_id) references tag_taxonomy_node (id),
    constraint ck_tag_taxonomy_node_order check (order_num >= 0),
    constraint ck_tag_taxonomy_node_version check (version >= 0)
);

create table tag_taxonomy_tag (
    id char(36) primary key,
    node_id char(36) not null,
    tag_id char(36) not null,
    order_num integer not null,
    create_time timestamp(6) not null,
    constraint uk_tag_taxonomy_tag unique (node_id, tag_id),
    constraint fk_tag_taxonomy_tag_node foreign key (node_id) references tag_taxonomy_node (id) on delete cascade,
    constraint fk_tag_taxonomy_tag_tag foreign key (tag_id) references tag_definition (id) on delete cascade,
    constraint ck_tag_taxonomy_tag_order check (order_num >= 0)
);

alter table tag_set add constraint fk_tag_set_default_tag foreign key (default_tag_id) references tag_definition (id);
alter table tag_definition add constraint fk_tag_definition_published_rule foreign key (published_rule_id) references tag_rule (id);
alter table tag_rule add constraint fk_tag_rule_root_node foreign key (root_node_id) references tag_rule_node (id);
