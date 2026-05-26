package io.kudos.ms.sys.common.datasource.api

import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * Data source external API.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysDataSourceApi {

    /**
     * Returns the data source (from cache) for the given tenant id and atomic service code.
     *
     * @param tenantId tenant id
     * @param atomicServiceCode atomic service code; defaults to null. When null, the caller must ensure the data source is unique for the given tenantId, otherwise an arbitrary one will be returned.
     * @return SysDataSourceCacheEntry, or null if not found
     */
    @GetMapping("/api/internal/sys/dataSource/getDataSource")
    fun getDataSourceFromCache(
        @RequestParam tenantId: String,
        @RequestParam(required = false) atomicServiceCode: String? = null
    ): SysDataSourceCacheEntry?


}
