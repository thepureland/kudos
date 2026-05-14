package io.kudos.ms.sys.common.tenant.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 租户错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysTenantErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键查找租户失败 */
    TENANT_NOT_FOUND("TENANT_NOT_FOUND", "租户不存在"),

    /** 租户名称已被占用（与 sys_tenant.name 的业务唯一性对应） */
    TENANT_NAME_ALREADY_EXISTS("TENANT_NAME_ALREADY_EXISTS", "租户名称已存在");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.tenant"

}
