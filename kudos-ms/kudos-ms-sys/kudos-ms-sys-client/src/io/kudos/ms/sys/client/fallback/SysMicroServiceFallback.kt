package io.kudos.ms.sys.client.fallback

import io.kudos.ms.sys.client.proxy.ISysMicroServiceProxy
import org.springframework.stereotype.Component


/**
 * 微服务容错处理
 *
 * @author K
 * @since 1.0.0
 */
@Component
interface SysMicroServiceFallback : ISysMicroServiceProxy {



}