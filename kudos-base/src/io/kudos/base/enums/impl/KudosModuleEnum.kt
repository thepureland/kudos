package io.kudos.base.enums.impl

import io.kudos.base.enums.ienums.IModuleEnum

/**
 * Module enum for the kudos framework.
 *
 * @author K
 * @since 1.0.0
 */
@Deprecated("Each service should define its own")
enum class KudosModuleEnum(
    override val code: String,
    override val displayText: String
) : IModuleEnum {

    /** Log audit module. */
    LOG_AUDIT("log_audit", "Log Audit")

}