package io.kudos.ms.user.common.enums.protection

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 账户保护错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class UserAccountProtectionErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.protection"

}
