package io.kudos.ms.user.common.login.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 记住登录错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class UserLoginRememberMeErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键或 (tenant_id, username) 查找记住我记录失败 */
    REMEMBER_ME_NOT_FOUND("REMEMBER_ME_NOT_FOUND", "记住我记录不存在"),

    /** 提供的记住我 token 已失效或被吊销 */
    REMEMBER_ME_TOKEN_INVALID("REMEMBER_ME_TOKEN_INVALID", "记住我 token 已失效");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.loginremember"

}
