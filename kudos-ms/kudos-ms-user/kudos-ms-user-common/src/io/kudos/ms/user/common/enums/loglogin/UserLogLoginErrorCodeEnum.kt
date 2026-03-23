package io.kudos.ms.user.common.enums.loglogin

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 登录日志错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class UserLogLoginErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.loglogin"

}
