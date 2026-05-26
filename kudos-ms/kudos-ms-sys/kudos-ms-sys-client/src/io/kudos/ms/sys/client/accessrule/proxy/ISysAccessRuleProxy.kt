package io.kudos.ms.sys.client.accessrule.proxy

import io.kudos.ms.sys.client.accessrule.fallback.SysAccessRuleFallback
import io.kudos.ms.sys.common.accessrule.api.ISysAccessRuleApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * Access rule client proxy interface.
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-accessrule", fallback = SysAccessRuleFallback::class)
interface ISysAccessRuleProxy : ISysAccessRuleApi {



}