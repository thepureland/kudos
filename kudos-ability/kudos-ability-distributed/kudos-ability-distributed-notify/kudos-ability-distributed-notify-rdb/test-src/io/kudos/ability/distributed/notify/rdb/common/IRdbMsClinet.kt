package io.kudos.ability.distributed.notify.rdb.common

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(value = "rdbNotifyMs", path = "ms")
interface IRdbMsClinet {
    @RequestMapping("change")
    fun change(@RequestParam("key") key: String?): Boolean

    @get:RequestMapping("/key")
    val key: String?
}