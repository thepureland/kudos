package io.kudos.base.enums.ienums

/**
 * Error code enum.
 *
 * @author K
 * @since 1.0.0
 */
interface IErrorCodeEnum : IDictEnum {

    /** Default display text. */
    val defaultDisplayText: String

    /** Display text or i18n key. */
    override val displayText: String
        get() = if (i18nKeyPrefix.isBlank()) {
            defaultDisplayText
        } else {
            "$i18nKeyPrefix.$code"
        }

    /** Prefix for the i18n key. */
    val i18nKeyPrefix: String

}
