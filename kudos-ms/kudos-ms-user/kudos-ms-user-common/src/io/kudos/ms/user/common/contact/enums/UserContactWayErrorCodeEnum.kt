package io.kudos.ms.user.common.contact.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 联系方式错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class UserContactWayErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键查找联系方式失败 */
    CONTACT_WAY_NOT_FOUND("CONTACT_WAY_NOT_FOUND", "联系方式不存在"),

    /** (user_id, contact_way_dict_code, contact_way_value) 已存在 */
    CONTACT_WAY_ALREADY_EXISTS("CONTACT_WAY_ALREADY_EXISTS", "该联系方式已绑定");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.contact"

}
