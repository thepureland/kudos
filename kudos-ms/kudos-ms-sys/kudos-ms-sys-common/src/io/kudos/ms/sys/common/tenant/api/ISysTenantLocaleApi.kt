package io.kudos.ms.sys.common.tenant.api

import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam


/**
 * Tenant-locale relation external API.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysTenantLocaleApi {


    @GetExchange("/api/internal/sys/tenantLocale/getLocaleCodesByTenantId")
    fun getLocaleCodesByTenantId(@RequestParam tenantId: String): Set<String>

    @GetExchange("/api/internal/sys/tenantLocale/getTenantIdsByLocaleCode")
    fun getTenantIdsByLocaleCode(@RequestParam localeCode: String): Set<String>

    @PostExchange("/api/internal/sys/tenantLocale/batchBind")
    fun batchBind(@RequestParam tenantId: String, @RequestBody localeCodes: Collection<String>): Int

    @PostExchange("/api/internal/sys/tenantLocale/unbind")
    fun unbind(@RequestParam tenantId: String, @RequestParam localeCode: String): Boolean

    @GetExchange("/api/internal/sys/tenantLocale/exists")
    fun exists(@RequestParam tenantId: String, @RequestParam localeCode: String): Boolean


}
