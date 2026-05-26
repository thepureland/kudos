package io.kudos.ms.sys.common.locale.api

import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * External API for language/locale dictionary.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysLocaleApi {

    /**
     * Query an enabled locale by language code.
     *
     * @param code Language code (e.g. zh_CN)
     * @return The locale entry; returns null if not found or not enabled.
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/sys/locale/getLocaleByCode")
    fun getLocaleByCode(@RequestParam code: String): SysLocaleCacheEntry?

    /**
     * List all enabled locales (ordered by sort_no ascending).
     *
     * @return List of enabled locales.
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/sys/locale/listActiveLocales")
    fun listActiveLocales(): List<SysLocaleCacheEntry>

}
