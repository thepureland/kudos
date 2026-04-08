-- 视图 v_sys_access_rule_with_ip：sys_access_rule 左连接 sys_access_rule_ip；首列 id = COALESCE(ip.id, ar.id)

CREATE OR REPLACE VIEW v_sys_access_rule_with_ip AS
SELECT COALESCE(ip."id", ar."id") AS "id",
       ar."id"          AS parent_id,
       ar."tenant_id",
       t."name"         AS tenant_name,
       ar."system_code",
       ar."access_rule_type_dict_code",
       ar."remark"           AS parent_remark,
       ar."active"           AS parent_active,
       ar."built_in"         AS parent_built_in,
       ar."create_user_id"   AS parent_create_user_id,
       ar."create_user_name" AS parent_create_user_name,
       ar."create_time"      AS parent_create_time,
       ar."update_user_id"   AS parent_update_user_id,
       ar."update_user_name" AS parent_update_user_name,
       ar."update_time"      AS parent_update_time,
       ip."id"               AS ip_id,
       ip."ip_start",
       ip."ip_end",
       ip."ip_type_dict_code",
       ip."expiration_time",
       ip."parent_rule_id",
       ip."remark",
       ip."active",
       ip."built_in",
       ip."create_user_id",
       ip."create_user_name",
       ip."create_time",
       ip."update_user_id",
       ip."update_user_name",
       ip."update_time"
FROM "sys_access_rule" ar
         LEFT JOIN "sys_access_rule_ip" ip
                   ON ip."parent_rule_id" = ar."id"
         LEFT JOIN "sys_tenant" t
                   ON ar."tenant_id" = t."id";
