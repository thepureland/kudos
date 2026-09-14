CREATE OR REPLACE VIEW "v_sys_dict_item" AS
SELECT di."id",
       di."item_code",
       di."item_name",
       di."dict_id",
       di."order_num",
       di."parent_id",
       di."remark",
       di."active",
       di."built_in",
       di."create_user_id",
       di."create_user_name",
       di."create_time",
       di."update_user_id",
       di."update_user_name",
       di."update_time",
       d."dict_type",
       d."dict_name",
       d."atomic_service_code"
FROM "sys_dict_item" di
         LEFT JOIN "sys_dict" d ON d."id" = di."dict_id";
