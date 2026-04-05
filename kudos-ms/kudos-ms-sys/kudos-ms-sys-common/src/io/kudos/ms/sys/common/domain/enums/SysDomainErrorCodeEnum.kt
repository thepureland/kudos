package io.kudos.ms.sys.common.domain.enums
import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 域名错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysDomainErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.domain"

}
