package io.kudos.ability.log.audit.common.enums

import io.kudos.base.enums.ienums.IDictEnum
import io.kudos.base.enums.ienums.IDictTypeEnum

/**
 * 操作类型
 * Create by (admin) on 3/4/15.
 */
enum class OperationTypeEnum(
    override val code: String,
    override val trans: String
) : IDictEnum {

    //Module.LOG op_type
    //注意:code一定要是数字
    OTHER("0", "other"),  //其它
    QUERY("1", "query"),  //查询
    CREATE("2", "create"),  //添加
    UPDATE("3", "update"),  //编辑
    DELETE("4", "delete"),  //删除
    LOGIN("5", "login"),  //登录
    LOGOUT("6", "logout"); //登出;

    val dictType: IDictTypeEnum
        get() = LogAuditDictTypeEnum.OPERATION_TYPE

}

