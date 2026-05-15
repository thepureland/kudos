package io.kudos.ms.user.common.account.enums

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
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按 (user_id, org_id) 查找机构-用户关系失败 */
    ORG_USER_NOT_FOUND("ORG_USER_NOT_FOUND", "机构-用户关系不存在"),

    /** (user_id, org_id) 已存在绑定 */
    ORG_USER_ALREADY_EXISTS("ORG_USER_ALREADY_EXISTS", "该用户已绑定到该机构");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.orguser"

}
