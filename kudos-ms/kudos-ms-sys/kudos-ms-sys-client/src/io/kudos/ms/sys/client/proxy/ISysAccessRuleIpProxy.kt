package io.kudos.ms.sys.client.proxy

import io.kudos.ms.sys.client.fallback.SysAccessRuleIpFallback
import io.kudos.ms.sys.common.api.ISysAccessRuleIpApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * ip访问规则客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-accessruleip", fallback = SysAccessRuleIpFallback::class)
interface ISysAccessRuleIpProxy : ISysAccessRuleIpApi {



}