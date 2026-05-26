package io.kudos.ability.log.audit.common.enums

import io.kudos.base.enums.ienums.IDictEnum
import io.kudos.base.enums.ienums.IDictTypeEnum

/**
 * Audit operation-type enum.
 *
 * **`code` must be a pure-numeric string** — [BaseLog.toSysLogVo] uses
 * `Integer.valueOf(op.code)` when writing `SysAuditLogVo.operateTypeId`;
 * non-numeric strings will throw NumberFormatException.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class OperationTypeEnum(
    override val code: String,
    override val displayText: String
) : IDictEnum {

    //Module.LOG op_type
    //Note: code must be numeric.
    OTHER("0", "other"),
    QUERY("1", "query"),
    CREATE("2", "create"),
    UPDATE("3", "update"),
    DELETE("4", "delete"),
    LOGIN("5", "login"),
    LOGOUT("6", "logout");

    val dictType: IDictTypeEnum
        get() = LogAuditDictTypeEnum.OPERATION_TYPE

}

