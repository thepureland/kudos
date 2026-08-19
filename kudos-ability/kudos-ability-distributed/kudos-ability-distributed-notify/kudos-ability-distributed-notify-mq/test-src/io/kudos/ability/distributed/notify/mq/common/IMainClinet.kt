package io.kudos.ability.distributed.notify.mq.common

import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.bind.annotation.RequestParam

interface IMainClinet {
    @GetExchange("/main/collection")
    fun collection(
        @RequestParam("port") port: Int?,
        @RequestParam("appKey") appKey: String?,
        @RequestParam("key") key: String?
    ): Boolean

    @GetExchange("/main/change")
    fun change(@RequestParam("key") key: String?): Boolean

    @GetExchange("/main/sync")
    fun sync(@RequestParam("key") key: String?): Boolean

    @GetExchange("/main/registry")
    fun registry(@RequestParam("appKey") appKey: String?, @RequestParam("port") port: Int?): Boolean
}