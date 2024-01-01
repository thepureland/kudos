CREATE TABLE IF NOT EXISTS test_table
(
    id            smallint not null,
    name          varchar(255) NOT NULL,
    birthday      datetime,
    active boolean,
    weight        double,
    height        smallint,
    PRIMARY KEY (id)
) comment = '测试表';