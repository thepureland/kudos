package io.kudos.ms.sys.client.microservice.fallback

import io.kudos.ms.sys.client.microservice.proxy.ISysSubSystemMicroServiceProxy
import org.springframework.stereotype.Component


/**
 * 子系统-微服务关系容错处理
 *
 * @author K
 * @since 1.0.0
 */
@Component
interface SysSubSystemMicroServiceFallback : ISysSubSystemMicroServiceProxy {



}