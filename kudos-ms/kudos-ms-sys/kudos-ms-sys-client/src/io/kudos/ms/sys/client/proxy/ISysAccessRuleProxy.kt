package io.kudos.ms.sys.client.proxy

import io.kudos.ms.sys.client.fallback.SysAccessRuleFallback
import io.kudos.ms.sys.common.api.ISysAccessRuleApi
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