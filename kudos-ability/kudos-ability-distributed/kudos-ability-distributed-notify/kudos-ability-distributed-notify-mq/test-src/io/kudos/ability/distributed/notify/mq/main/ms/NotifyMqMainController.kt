package io.kudos.ability.distributed.notify.mq.main.ms

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/main")
open class NotifyMqMainController {

    @Autowired
    private lateinit var notifyMqMsService: NotifyMqMsService

    @RequestMapping("/collection")
    fun collection(
        @RequestParam("port") port: Int,
        @RequestParam("appKey") appKey: String,
        @RequestParam("key") key: String
    ): Boolean {
        return notifyMqMsService.collection(port, appKey, key)
    }

    @RequestMapping("/change")
    fun change(@RequestParam("key") key: String): Boolean {
        return notifyMqMsService.process(key)
    }

    @RequestMapping("/sync")
    fun sync(@RequestParam("key") key: String): Boolean {
        return notifyMqMsService.isSync(key)
    }

    @RequestMapping("/registry")
    fun registry(@RequestParam("appKey") appKey: String, @RequestParam("port") port: Int?): Boolean {
        return notifyMqMsService.registry(appKey, port)
    }
}
