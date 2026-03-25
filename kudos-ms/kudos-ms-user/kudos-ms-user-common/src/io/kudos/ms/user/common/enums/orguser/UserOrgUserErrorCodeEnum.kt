package io.kudos.ms.user.common.enums.orguser

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 机构用户关系错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class UserOrgUserErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.orguser"

}
