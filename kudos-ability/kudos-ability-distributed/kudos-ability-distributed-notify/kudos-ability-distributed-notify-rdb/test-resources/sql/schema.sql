CREATE TABLE IF NOT exists"sys_app" (
    "id" varchar(36) DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    "app_name" varchar(64) NOT NULL,
    "protocol" varchar(10) NOT NULL,
    "ip" varchar(128) NOT NULL,
    "port" int4 NOT NULL,
    "frequency" int2 DEFAULT 1,
    "status" varchar(32) DEFAULT 'active',
    "last_heart_time" timestamp DEFAULT now()
    );
