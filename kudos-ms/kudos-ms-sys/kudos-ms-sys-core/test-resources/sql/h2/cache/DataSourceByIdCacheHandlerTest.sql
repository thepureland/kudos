merge into "sys_data_source" ("id", "name", "sub_system_code", "micro_service_code",
                               "tenant_id", "url", "username", "password", "active", "built_in")
values ('3d2acef6-e828-43c5-a512-111111111111', 'test_ds_1', 'default', 'default', null,
        'jdbc:h2:tcp://localhost:1521/mem:test;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;', 'sa', 'sa', true, false),
       ('3d2acef6-e828-43c5-a512-222222222222', 'test_ds_2', 'default', 'default', null,
        'jdbc:h2:tcp://localhost:1521/mem:test;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;', 'sa', 'sa', true, false),
       ('3d2acef6-e828-43c5-a512-333333333333', 'test_ds_3', 'default', 'default', null,
        'jdbc:h2:tcp://localhost:1521/mem:test;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;', 'sa', 'sa', true, false),
       ('3d2acef6-e828-43c5-a512-444444444444', 'test_ds_4', 'default', 'default', null,
        'jdbc:h2:tcp://localhost:1521/mem:test;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;', 'sa', 'sa', true, false),
       ('3d2acef6-e828-43c5-a512-555555555555', 'test_ds_5', 'default', 'default', null,
        'jdbc:h2:tcp://localhost:1521/mem:test;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;', 'sa', 'sa', true, false);
