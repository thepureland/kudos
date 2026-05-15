package io.kudos.ms.auth.common.role.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 角色用户关系错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class AuthRoleUserErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按 (role_id, user_id) 查找角色-用户关系失败 */
    ROLE_USER_NOT_FOUND("ROLE_USER_NOT_FOUND", "角色-用户关系不存在"),

    /** (role_id, user_id) 已存在绑定 */
    ROLE_USER_ALREADY_EXISTS("ROLE_USER_ALREADY_EXISTS", "该用户已拥有该角色");

    override val i18nKeyPrefix: String
        get() = "auth.error-msg.roleuser"

}
