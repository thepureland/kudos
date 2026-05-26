package io.kudos.ms.sys.common.tenant.api

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam


/**
 * Tenant-system relation external API.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysTenantSystemApi {


    @GetMapping("/api/internal/sys/tenantSystem/searchSystemCodesByTenantId")
    fun searchSystemCodesByTenantId(@RequestParam tenantId: String): Set<String>

    @GetMapping("/api/internal/sys/tenantSystem/searchTenantIdsBySystemCode")
    fun searchTenantIdsBySystemCode(@RequestParam systemCode: String): Set<String>

    @PostMapping("/api/internal/sys/tenantSystem/groupingSystemCodesByTenantIds")
    fun groupingSystemCodesByTenantIds(@RequestBody tenantIds: Collection<String>? = null): Map<String, List<String>>

    @PostMapping("/api/internal/sys/tenantSystem/groupingTenantIdsBySystemCodes")
    fun groupingTenantIdsBySystemCodes(@RequestBody systemCodes: Collection<String>? = null): Map<String, List<String>>

    @PostMapping("/api/internal/sys/tenantSystem/batchBind")
    fun batchBind(@RequestParam tenantId: String, @RequestBody systemCodes: Collection<String>): Int

    @PostMapping("/api/internal/sys/tenantSystem/unbind")
    fun unbind(@RequestParam tenantId: String, @RequestParam systemCode: String): Boolean

    @GetMapping("/api/internal/sys/tenantSystem/exists")
    fun exists(@RequestParam tenantId: String, @RequestParam systemCode: String): Boolean

    @DeleteMapping("/api/internal/sys/tenantSystem/deleteByTenantId")
    fun deleteByTenantId(@RequestParam tenantId: String): Int

    @PostMapping("/api/internal/sys/tenantSystem/batchDeleteByTenantIds")
    fun batchDeleteByTenantIds(@RequestBody tenantIds: Collection<String>): Int


}
