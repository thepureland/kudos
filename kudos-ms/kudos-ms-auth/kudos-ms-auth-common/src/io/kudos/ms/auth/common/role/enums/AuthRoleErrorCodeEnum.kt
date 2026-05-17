package io.kudos.ms.auth.common.role.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 角色错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class AuthRoleErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键或 (tenant_id, code) 维度查找角色失败 */
    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "角色不存在"),

    /** (tenant_id, code) 已被占用 */
    ROLE_CODE_ALREADY_EXISTS("ROLE_CODE_ALREADY_EXISTS", "该租户下角色编码已存在");

    override val i18nKeyPrefix: String
        get() = "auth.error-msg.role"

}
