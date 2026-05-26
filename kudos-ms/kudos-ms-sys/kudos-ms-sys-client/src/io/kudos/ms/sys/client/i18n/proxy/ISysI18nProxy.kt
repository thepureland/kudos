package io.kudos.ms.sys.client.i18n.proxy

import io.kudos.ms.sys.client.i18n.fallback.SysI18nFallback
import io.kudos.ms.sys.common.i18n.api.ISysI18nApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * I18n client proxy interface.
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-i18n", fallback = SysI18nFallback::class)
interface ISysI18nProxy : ISysI18nApi {



}