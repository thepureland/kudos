package io.kudos.ms.sys.common.microservice.api

import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam


/**
 * External API for subsystem-microservice relationship.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysSubSystemMicroServiceApi {


    @GetExchange("/api/internal/sys/subSystemMicroService/getMicroServiceCodesBySubSystemCode")
    fun getMicroServiceCodesBySubSystemCode(@RequestParam subSystemCode: String): Set<String>

    @GetExchange("/api/internal/sys/subSystemMicroService/getSubSystemCodesByMicroServiceCode")
    fun getSubSystemCodesByMicroServiceCode(@RequestParam microServiceCode: String): Set<String>

    @PostExchange("/api/internal/sys/subSystemMicroService/batchBind")
    fun batchBind(@RequestParam subSystemCode: String, @RequestBody microServiceCodes: Collection<String>): Int

    @PostExchange("/api/internal/sys/subSystemMicroService/unbind")
    fun unbind(@RequestParam subSystemCode: String, @RequestParam microServiceCode: String): Boolean

    @GetExchange("/api/internal/sys/subSystemMicroService/exists")
    fun exists(@RequestParam subSystemCode: String, @RequestParam microServiceCode: String): Boolean


}
