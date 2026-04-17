package io.kudos.ms.sys.common.accessrule.enums

import io.kudos.base.enums.ienums.IDictEnum

/**
 * IP类型枚举
 *
 * @author K
 * @since 1.0.0
 */
enum class IpTypeEnum(override val code: String, override val displayText: String): IDictEnum {

    IPV4("ipv4", "IPV4"),
    IPV6("ipv6", "IPV6"),

}