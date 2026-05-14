package io.kudos.ms.sys.common.microservice.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 微服务错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysMicroServiceErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键查找微服务失败（注：微服务 PK 即 code） */
    MICRO_SERVICE_NOT_FOUND("MICRO_SERVICE_NOT_FOUND", "微服务不存在"),

    /** 微服务编码已被占用 */
    MICRO_SERVICE_CODE_ALREADY_EXISTS("MICRO_SERVICE_CODE_ALREADY_EXISTS", "微服务编码已存在");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.microservice"

}
