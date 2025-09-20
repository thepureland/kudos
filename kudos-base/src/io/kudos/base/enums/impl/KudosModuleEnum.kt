package io.kudos.base.enums.impl

import io.kudos.base.enums.ienums.IModuleEnum

/**
 * kudos框架的模块枚举
 *
 * @author K
 * @since 1.0.0
 */
enum class KudosModuleEnum(
    override val code: String,
    override val trans: String
) : IModuleEnum {

    /** 日志审计模块 */
    LOG_AUDIT("log_audit", "日志审计")

}