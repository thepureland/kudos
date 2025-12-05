CREATE OR REPLACE VIEW v_sys_access_rule_with_ip AS
SELECT ar."id"          AS parent_id,
       ar."tenant_id",
       ar."sub_system_code",
       ar."portal_code",
       ar."rule_type_dict_code",
       ar."remark"      AS parent_remark,
       ar."active"      AS parent_active,
       ar."built_in"    AS parent_built_in,
       ar."create_user" AS parent_create_user,
       ar."create_time" AS parent_create_time,
       ar."update_user" AS parent_update_user,
       ar."update_time" AS parent_update_time,
       ip."id",
       ip."ip_start",
       ip."ip_end",
       ip."ip_type_dict_code",
       ip."expiration_time",
       ip."parent_rule_id",
       ip."remark",
       ip."active",
       ip."built_in",
       ip."create_user",
       ip."create_time",
       ip."update_user",
       ip."update_time"
FROM "sys_access_rule" ar
         LEFT JOIN "sys_access_rule_ip" ip
                   ON ip."parent_rule_id" = ar."id";