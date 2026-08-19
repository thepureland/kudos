package io.kudos.ms.sys.common.microservice.api

import io.kudos.ms.sys.common.microservice.vo.SysMicroServiceCacheEntry
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PutExchange
import org.springframework.web.bind.annotation.RequestParam


/**
 * External API for microservices.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysMicroServiceApi {

    @GetExchange("/api/internal/sys/microService/getMicroService")
    fun getMicroServiceFromCache(@RequestParam code: String): SysMicroServiceCacheEntry?

    @GetExchange("/api/internal/sys/microService/listAll")
    fun getAllMicroServicesFromCache(): List<SysMicroServiceCacheEntry>

    @GetExchange("/api/internal/sys/microService/listExcludeAtomic")
    fun getMicroServicesExcludeAtomicFromCache(): List<SysMicroServiceCacheEntry>

    @GetExchange("/api/internal/sys/microService/listAtomic")
    fun getAtomicServicesFromCache(): List<SysMicroServiceCacheEntry>

    @GetExchange("/api/internal/sys/microService/listSubByParent")
    fun getSubMicroServicesFromCache(@RequestParam parentCode: String): List<SysMicroServiceCacheEntry>

    @GetExchange("/api/internal/sys/microService/listAtomicByParent")
    fun getAtomicServicesByParentCodeFromCache(@RequestParam parentCode: String): List<SysMicroServiceCacheEntry>

    @PutExchange("/api/internal/sys/microService/updateActive")
    fun updateActive(@RequestParam code: String, @RequestParam active: Boolean): Boolean


}
