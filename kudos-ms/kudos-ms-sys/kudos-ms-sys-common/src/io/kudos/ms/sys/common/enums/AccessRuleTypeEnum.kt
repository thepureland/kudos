package io.kudos.ms.sys.common.enums

import io.kudos.base.enums.ienums.IDictEnum

/**
 * 访问规则类型枚举
 *
 * @author K
 * @since 1.0.0
 */
enum class AccessRuleTypeEnum(override val code: String, override val trans: String): IDictEnum {

    UNLIMITED("1", "不限制"),
    WHITELIST("2", "白名单"),
    BLACKLIST("3", "黑名单"),
    WHITELIST_BLACKLIST("4", "白名单+黑名单")

}