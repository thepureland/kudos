package io.kudos.ms.sys.common.microservice.api

import io.kudos.ms.sys.common.microservice.vo.SysMicroServiceCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * External API for microservices.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysMicroServiceApi {

    @GetMapping("/api/internal/sys/microService/getMicroService")
    fun getMicroServiceFromCache(@RequestParam code: String): SysMicroServiceCacheEntry?

    @GetMapping("/api/internal/sys/microService/listAll")
    fun getAllMicroServicesFromCache(): List<SysMicroServiceCacheEntry>

    @GetMapping("/api/internal/sys/microService/listExcludeAtomic")
    fun getMicroServicesExcludeAtomicFromCache(): List<SysMicroServiceCacheEntry>

    @GetMapping("/api/internal/sys/microService/listAtomic")
    fun getAtomicServicesFromCache(): List<SysMicroServiceCacheEntry>

    @GetMapping("/api/internal/sys/microService/listSubByParent")
    fun getSubMicroServicesFromCache(@RequestParam parentCode: String): List<SysMicroServiceCacheEntry>

    @GetMapping("/api/internal/sys/microService/listAtomicByParent")
    fun getAtomicServicesByParentCodeFromCache(@RequestParam parentCode: String): List<SysMicroServiceCacheEntry>

    @PutMapping("/api/internal/sys/microService/updateActive")
    fun updateActive(@RequestParam code: String, @RequestParam active: Boolean): Boolean


}
