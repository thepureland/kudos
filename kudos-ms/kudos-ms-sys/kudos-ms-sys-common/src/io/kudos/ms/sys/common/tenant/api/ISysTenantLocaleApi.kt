package io.kudos.ms.sys.common.tenant.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam


/**
 * 租户-语言关系 对外API
 *
 * @author K
 * @since 1.0.0
 */
interface ISysTenantLocaleApi {


    @GetMapping("/api/internal/sys/tenantLocale/getLocaleCodesByTenantId")
    fun getLocaleCodesByTenantId(@RequestParam tenantId: String): Set<String>

    @GetMapping("/api/internal/sys/tenantLocale/getTenantIdsByLocaleCode")
    fun getTenantIdsByLocaleCode(@RequestParam localeCode: String): Set<String>

    @PostMapping("/api/internal/sys/tenantLocale/batchBind")
    fun batchBind(@RequestParam tenantId: String, @RequestBody localeCodes: Collection<String>): Int

    @PostMapping("/api/internal/sys/tenantLocale/unbind")
    fun unbind(@RequestParam tenantId: String, @RequestParam localeCode: String): Boolean

    @GetMapping("/api/internal/sys/tenantLocale/exists")
    fun exists(@RequestParam tenantId: String, @RequestParam localeCode: String): Boolean


}
