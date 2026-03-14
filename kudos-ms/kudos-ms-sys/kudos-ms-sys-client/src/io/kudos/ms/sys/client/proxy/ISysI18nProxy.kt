package io.kudos.ms.sys.client.proxy

import io.kudos.ms.sys.client.fallback.SysI18nFallback
import io.kudos.ms.sys.common.api.ISysI18nApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 国际化客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-i18n", fallback = SysI18nFallback::class)
interface ISysI18nProxy : ISysI18nApi {



}