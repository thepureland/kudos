package io.kudos.ms.sys.client.accessrule.proxy

import io.kudos.ms.sys.client.accessrule.fallback.SysAccessRuleFallback
import io.kudos.ms.sys.common.accessrule.api.ISysAccessRuleApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 访问规则客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-accessrule", fallback = SysAccessRuleFallback::class)
interface ISysAccessRuleProxy : ISysAccessRuleApi {



}