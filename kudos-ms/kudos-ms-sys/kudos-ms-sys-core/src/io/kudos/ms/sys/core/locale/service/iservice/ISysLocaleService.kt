package io.kudos.ms.sys.core.locale.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import io.kudos.ms.sys.core.locale.model.po.SysLocale


/**
 * Business interface for the language/locale dictionary.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysLocaleService : IBaseCrudService<String, SysLocale> {

    /**
     * Look up an active locale by code.
     *
     * @param code locale code, non-blank
     * @return the active locale, or `null` if not found or not active
     */
    fun getLocaleByCode(code: String): SysLocaleCacheEntry?

    /**
     * List all active locales (ordered by sort_no ascending).
     */
    fun listActiveLocales(): List<SysLocaleCacheEntry>

    /**
     * Update the active status.
     *
     * @param id primary key
     * @param active whether to enable
     * @return whether the update succeeded
     */
    fun updateActive(id: String, active: Boolean): Boolean

}
