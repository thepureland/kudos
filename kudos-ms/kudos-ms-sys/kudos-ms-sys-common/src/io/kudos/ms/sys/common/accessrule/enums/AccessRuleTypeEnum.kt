package io.kudos.ms.sys.common.accessrule.enums

import io.kudos.base.enums.ienums.IDictEnum

/**
 * Access rule type enum.
 *
 * @author K
 * @since 1.0.0
 */
enum class AccessRuleTypeEnum(override val code: String, override val displayText: String): IDictEnum {

    UNLIMITED("1", "Unlimited"),
    WHITELIST("2", "Whitelist"),
    BLACKLIST("3", "Blacklist"),
    WHITELIST_BLACKLIST("4", "Whitelist + Blacklist")

}