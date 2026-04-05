package io.kudos.ms.sys.client.datasource.fallback

import io.kudos.ms.sys.client.datasource.proxy.ISysDataSourceProxy
import org.springframework.stereotype.Component


/**
 * 数据源容错处理
 *
 * @author K
 * @since 1.0.0
 */
@Component
interface SysDataSourceFallback : ISysDataSourceProxy {



}