package io.kudos.ability.log.audit.common.enums

import io.kudos.base.enums.ienums.IDictEnum
import io.kudos.base.enums.ienums.IDictTypeEnum

/**
 * 审计操作类型枚举。
 *
 * **code 必须是纯数字字符串**——[BaseLog.toSysLogVo] 用 `Integer.valueOf(op.code)` 写入
 * `SysAuditLogVo.operateTypeId`，非数字字符串会抛 NumberFormatException。
 *
 * @author K
 * @since 1.0.0
 */
enum class OperationTypeEnum(
    override val code: String,
    override val displayText: String
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

