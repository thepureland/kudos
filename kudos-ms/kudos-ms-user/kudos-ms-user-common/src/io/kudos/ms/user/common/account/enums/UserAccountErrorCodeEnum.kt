package io.kudos.ms.user.common.account.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 用户账户错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class UserAccountErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键或 (tenant_id, username) 维度查找用户失败 */
    USER_NOT_FOUND("USER_NOT_FOUND", "用户不存在"),

    /** (tenant_id, username) 已被占用 */
    USERNAME_ALREADY_EXISTS("USERNAME_ALREADY_EXISTS", "该租户下用户名已存在");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.user"

}
