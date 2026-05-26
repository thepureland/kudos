package io.kudos.ms.sys.core.locale.api

import io.kudos.ms.sys.common.locale.api.ISysLocaleApi
import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import io.kudos.ms.sys.core.locale.service.iservice.ISysLocaleService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component


/**
 * Local implementation of the language/locale dictionary API.
 *
 * @author K
 * @since 1.0.0
 */
@Primary
@Component
open class SysLocaleApi(
    private val sysLocaleService: ISysLocaleService,
) : ISysLocaleApi {

    override fun getLocaleByCode(code: String): SysLocaleCacheEntry? = sysLocaleService.getLocaleByCode(code)

    override fun listActiveLocales(): List<SysLocaleCacheEntry> = sysLocaleService.listActiveLocales()
}
