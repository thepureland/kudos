merge into "sys_access_rule" ("id", "tenant_id", "system_code", "rule_type_dict_code", "remark", "active", "built_in")
    values ('8026f3ac-563b-4545-88dc-b8f70ea48081', 'tenantId-1', 'subSys-a', '0', null, true, false),
           ('8026f3ac-563b-4545-88dc-b8f70ea48082', 'tenantId-2', 'subSys-a', '1', null, true, false),
           ('8026f3ac-563b-4545-88dc-b8f70ea48083', 'tenantId-3', 'subSys-a', '2', null, true, false),
           ('8026f3ac-563b-4545-88dc-b8f70ea48084', 'tenantId-4', 'subSys-a', '3', null, true, false),
           ('8026f3ac-563b-4545-88dc-b8f70ea48085', 'tenantId-5', 'subSys-b', '0', null, true, false),
           ('8026f3ac-563b-4545-88dc-b8f70ea48086', 'tenantId-6', 'subSys-b', '0', null, true, false),
           ('8026f3ac-563b-4545-88dc-b8f70ea48087', null, 'subSys-c', '0', null, true, false),
           ('8026f3ac-563b-4545-88dc-b8f70ea48088', null, 'subSys-d', '0', null, true, false),
           ('8026f3ac-563b-4545-88dc-b8f70ea48089', null, 'subSys-e', '0', null, true, false),
           ('8026f3ac-563b-4545-88dc-b8f70ea48080', null, 'subSys-f', '0', null, false, false);



merge into "sys_access_rule_ip" ("id", "ip_start", "ip_end", "ip_type_dict_code", "expiration_time", "parent_rule_id",
                                 "remark", "active", "built_in")
    values
        -- 192.168.0.1 ~ 192.168.0.128
        ('3a443825-4896-49e4-a304-e4e2ddadd705', 3232235521, 3232235648, '1', null, '8026f3ac-563b-4545-88dc-b8f70ea48082', null, true,
            false),
        -- 192.168.0.129
        ('3a443825-4896-49e4-a304-e4e2ddadd706', 3232235649, 3232235649, '1', null, '8026f3ac-563b-4545-88dc-b8f70ea48082', null, true,
            false),
        -- 192.168.0.139
        ('3a443825-4896-49e4-a304-e4e2ddadd707', 3232235659, 3232235659, '1', null, '8026f3ac-563b-4545-88dc-b8f70ea48082', null, false,
         false),
        -- 192.168.0.140 ~ 192.168.0.200
        ('3a443825-4896-49e4-a304-e4e2ddadd708', 3232235660, 3232235720, '2', null, '8026f3ac-563b-4545-88dc-b8f70ea48083', null, true,
            false),
        -- 192.168.0.201
        ('3a443825-4896-49e4-a304-e4e2ddadd709', 3232235721, 3232235721, '2', null, '8026f3ac-563b-4545-88dc-b8f70ea48083', null, true,
            false),
        -- 192.168.0.205 ~ 192.168.0.210
        ('3a443825-4896-49e4-a304-e4e2ddadd710', 3232235725, 3232235730, '1', null, '8026f3ac-563b-4545-88dc-b8f70ea48084', null, true,
            false),
        -- 192.168.0.210
        ('3a443825-4896-49e4-a304-e4e2ddadd711', 3232235730, 3232235730, '2', null, '8026f3ac-563b-4545-88dc-b8f70ea48084', null, false,
            false);
