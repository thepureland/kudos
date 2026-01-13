package io.kudos.ability.log.audit.common.enums

import io.kudos.base.enums.ienums.IDictTypeEnum
import io.kudos.base.enums.ienums.IModuleEnum
import io.kudos.base.enums.impl.KudosModuleEnum


/**
 * 日志审计字典类型枚举
 *
 * @author K
 * @since 1.0.0
 */
enum class LogAuditDictTypeEnum(
    override val module: IModuleEnum,
    override val type: String,
    override val desc: String,
    override val parentType: IDictTypeEnum? = null
) : IDictTypeEnum {

    /** 操作类型 */
    OPERATION_TYPE(KudosModuleEnum.LOG_AUDIT, "operation_type", "操作类型")

}