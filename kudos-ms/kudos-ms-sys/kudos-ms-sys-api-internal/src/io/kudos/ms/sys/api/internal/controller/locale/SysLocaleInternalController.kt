package io.kudos.ms.sys.api.internal.controller.locale

import io.kudos.ms.sys.common.locale.api.ISysLocaleApi
import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import io.kudos.ms.sys.core.locale.api.SysLocaleApi
import org.springframework.web.bind.annotation.RestController


/**
 * Language/locale dictionary internal RPC controller. Paths are inherited from method-level annotations on [ISysLocaleApi].
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysLocaleInternalController(
    private val sysLocaleApi: SysLocaleApi,
) : ISysLocaleApi {

    override fun getLocaleByCode(code: String): SysLocaleCacheEntry? =
        sysLocaleApi.getLocaleByCode(code)

    override fun listActiveLocales(): List<SysLocaleCacheEntry> =
        sysLocaleApi.listActiveLocales()

}
