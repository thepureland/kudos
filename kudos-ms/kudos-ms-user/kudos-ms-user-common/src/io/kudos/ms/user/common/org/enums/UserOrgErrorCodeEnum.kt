package io.kudos.ms.user.common.org.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 机构错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class UserOrgErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键查找机构失败 */
    ORG_NOT_FOUND("ORG_NOT_FOUND", "机构不存在"),

    /** (tenant_id, code) 已被占用 */
    ORG_CODE_ALREADY_EXISTS("ORG_CODE_ALREADY_EXISTS", "该租户下机构编码已存在"),

    /** 父机构不存在或已禁用 */
    PARENT_ORG_NOT_FOUND("PARENT_ORG_NOT_FOUND", "父机构不存在或已禁用");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.org"

}
