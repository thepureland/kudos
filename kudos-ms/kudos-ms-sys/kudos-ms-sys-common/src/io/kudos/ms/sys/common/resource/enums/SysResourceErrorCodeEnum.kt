package io.kudos.ms.sys.common.resource.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 资源错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysResourceErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键查找资源失败 */
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "资源不存在"),

    /** (sub_system_code, url) 已存在对应资源 */
    RESOURCE_URL_ALREADY_EXISTS("RESOURCE_URL_ALREADY_EXISTS", "该子系统下已存在相同 URL 的资源"),

    /** 父资源不存在或被禁用，无法挂载子资源 */
    PARENT_RESOURCE_NOT_FOUND("PARENT_RESOURCE_NOT_FOUND", "父资源不存在或已禁用");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.resource"

}
