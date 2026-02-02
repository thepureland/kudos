package io.kudos.ms.sys.client.proxy

import io.kudos.ms.sys.common.api.ISysAccessRuleIpApi
import io.kudos.ms.sys.client.fallback.SysAccessRuleIpFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * ip访问规则客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-accessruleip", fallback = SysAccessRuleIpFallback::class)
interface ISysAccessRuleIpProxy : ISysAccessRuleIpApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}