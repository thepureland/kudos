package io.kudos.ability.distributed.notify.mq.common

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(value = "notifyMain", path = "main")
interface IMainClinet {
    @RequestMapping("collection")
    fun collection(
        @RequestParam("port") port: Int?,
        @RequestParam("appKey") appKey: String?,
        @RequestParam("key") key: String?
    ): Boolean

    @RequestMapping("change")
    fun change(@RequestParam("key") key: String?): Boolean

    @RequestMapping("sync")
    fun sync(@RequestParam("key") key: String?): Boolean

    @RequestMapping("registry")
    fun registry(@RequestParam("appKey") appKey: String?, @RequestParam("port") port: Int?): Boolean
}