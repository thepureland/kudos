package io.kudos.ms.sys.client.tenant.fallback

import io.kudos.ms.sys.client.tenant.proxy.ISysTenantProxy
import org.springframework.stereotype.Component


/**
 * 租户容错处理
 *
 * @author K
 * @since 1.0.0
 */
@Component
interface SysTenantFallback : ISysTenantProxy {



}