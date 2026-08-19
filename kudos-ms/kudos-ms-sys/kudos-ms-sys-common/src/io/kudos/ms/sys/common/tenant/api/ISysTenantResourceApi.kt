package io.kudos.ms.sys.common.tenant.api

import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam


/**
 * Tenant-resource relation external API.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysTenantResourceApi {


    @GetExchange("/api/internal/sys/tenantResource/getResourceIdsByTenantId")
    fun getResourceIdsByTenantId(@RequestParam tenantId: String): Set<String>

    @GetExchange("/api/internal/sys/tenantResource/getTenantIdsByResourceId")
    fun getTenantIdsByResourceId(@RequestParam resourceId: String): Set<String>

    @PostExchange("/api/internal/sys/tenantResource/batchBind")
    fun batchBind(@RequestParam tenantId: String, @RequestBody resourceIds: Collection<String>): Int

    @PostExchange("/api/internal/sys/tenantResource/unbind")
    fun unbind(@RequestParam tenantId: String, @RequestParam resourceId: String): Boolean

    @GetExchange("/api/internal/sys/tenantResource/exists")
    fun exists(@RequestParam tenantId: String, @RequestParam resourceId: String): Boolean


}
