package io.kudos.ability.log.audit.common.enums

import io.kudos.base.enums.ienums.IDictTypeEnum
import io.kudos.base.enums.ienums.IModuleEnum
import io.kudos.base.enums.impl.KudosModuleEnum


/**
 * Log-audit dictionary-type enum.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class LogAuditDictTypeEnum(
    override val module: IModuleEnum,
    override val type: String,
    override val desc: String,
    override val parentType: IDictTypeEnum? = null
) : IDictTypeEnum {

    /** Operation type. */
    OPERATION_TYPE(KudosModuleEnum.LOG_AUDIT, "operation_type", "Operation type")

}