package io.kudos.ms.sys.client.accessrule.proxy

import io.kudos.ms.sys.client.accessrule.fallback.SysAccessRuleIpFallback
import io.kudos.ms.sys.common.accessrule.api.ISysAccessRuleIpApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * IP access rule client proxy interface.
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-accessruleip", fallback = SysAccessRuleIpFallback::class)
interface ISysAccessRuleIpProxy : ISysAccessRuleIpApi {



}