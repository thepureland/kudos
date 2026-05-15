package io.kudos.ms.user.common.account.enums

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
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按 user_id 查找账户保护记录失败 */
    PROTECTION_NOT_FOUND("PROTECTION_NOT_FOUND", "账户保护记录不存在"),

    /** 账户已被锁定 */
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "账户已被锁定"),

    /** 密码错误次数超出阈值 */
    PASSWORD_RETRY_EXCEEDED("PASSWORD_RETRY_EXCEEDED", "密码错误次数超出阈值");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.protection"

}
