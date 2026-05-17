package io.kudos.ms.auth.common.role.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 角色资源关系错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class AuthRoleResourceErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按 (role_id, resource_id) 查找角色-资源关系失败 */
    ROLE_RESOURCE_NOT_FOUND("ROLE_RESOURCE_NOT_FOUND", "角色-资源关系不存在"),

    /** (role_id, resource_id) 已存在绑定 */
    ROLE_RESOURCE_ALREADY_EXISTS("ROLE_RESOURCE_ALREADY_EXISTS", "该角色已拥有该资源");

    override val i18nKeyPrefix: String
        get() = "auth.error-msg.roleresource"

}
