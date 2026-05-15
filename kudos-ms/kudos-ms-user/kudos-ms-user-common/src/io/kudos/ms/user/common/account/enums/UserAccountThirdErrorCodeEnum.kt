package io.kudos.ms.user.common.account.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 第三方账户错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class UserAccountThirdErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键或 (provider, openid) 维度查找第三方绑定失败 */
    THIRD_ACCOUNT_NOT_FOUND("THIRD_ACCOUNT_NOT_FOUND", "第三方账号绑定不存在"),

    /** (provider, openid) 已存在绑定 */
    THIRD_ACCOUNT_ALREADY_BOUND("THIRD_ACCOUNT_ALREADY_BOUND", "该第三方账号已被绑定");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.user-third"

}
