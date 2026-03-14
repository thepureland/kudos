package io.kudos.ms.sys.client.fallback

import io.kudos.ms.sys.client.proxy.ISysTenantResourceProxy
import org.springframework.stereotype.Component


/**
 * 租户-资源关系容错处理
 *
 * @author K
 * @since 1.0.0
 */
@Component
interface SysTenantResourceFallback : ISysTenantResourceProxy {



}