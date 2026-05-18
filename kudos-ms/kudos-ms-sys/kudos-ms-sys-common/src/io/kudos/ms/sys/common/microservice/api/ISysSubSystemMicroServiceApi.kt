package io.kudos.ms.sys.common.microservice.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam


/**
 * 子系统-微服务关系 对外API
 *
 * @author K
 * @since 1.0.0
 */
interface ISysSubSystemMicroServiceApi {


    @GetMapping("/api/internal/sys/subSystemMicroService/getMicroServiceCodesBySubSystemCode")
    fun getMicroServiceCodesBySubSystemCode(@RequestParam subSystemCode: String): Set<String>

    @GetMapping("/api/internal/sys/subSystemMicroService/getSubSystemCodesByMicroServiceCode")
    fun getSubSystemCodesByMicroServiceCode(@RequestParam microServiceCode: String): Set<String>

    @PostMapping("/api/internal/sys/subSystemMicroService/batchBind")
    fun batchBind(@RequestParam subSystemCode: String, @RequestBody microServiceCodes: Collection<String>): Int

    @PostMapping("/api/internal/sys/subSystemMicroService/unbind")
    fun unbind(@RequestParam subSystemCode: String, @RequestParam microServiceCode: String): Boolean

    @GetMapping("/api/internal/sys/subSystemMicroService/exists")
    fun exists(@RequestParam subSystemCode: String, @RequestParam microServiceCode: String): Boolean


}
