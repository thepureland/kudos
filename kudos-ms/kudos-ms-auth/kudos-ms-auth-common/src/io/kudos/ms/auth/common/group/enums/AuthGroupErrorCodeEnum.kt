package io.kudos.ms.auth.common.group.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 组错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class AuthGroupErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键或 (tenant_id, code) 维度查找用户组失败 */
    GROUP_NOT_FOUND("GROUP_NOT_FOUND", "用户组不存在"),

    /** (tenant_id, code) 已被占用 */
    GROUP_CODE_ALREADY_EXISTS("GROUP_CODE_ALREADY_EXISTS", "该租户下用户组编码已存在");

    override val i18nKeyPrefix: String
        get() = "auth.error-msg.group"

}
