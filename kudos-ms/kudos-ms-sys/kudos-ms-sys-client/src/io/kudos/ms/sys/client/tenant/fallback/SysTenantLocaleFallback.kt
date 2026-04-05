package io.kudos.ms.sys.client.tenant.fallback

import io.kudos.ms.sys.client.tenant.proxy.ISysTenantLocaleProxy
import org.springframework.stereotype.Component


/**
 * 租户-语言关系容错处理
 *
 * @author K
 * @since 1.0.0
 */
@Component
interface SysTenantLocaleFallback : ISysTenantLocaleProxy {



}