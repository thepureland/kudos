package io.kudos.ability.distributed.notify.rdb.ms.common

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
open class NotifyRdbMsController {

    @Autowired
    private lateinit var notifyMqMsService: NotifyRdbMsService

    @Autowired
    private lateinit var rdbDataSourceNotifyListener: RdbDataSourceNotifyListener

    @RequestMapping("/change")
    fun change(@RequestParam("key") key: String?): Boolean {
        return notifyMqMsService.process(key)
    }

    @get:RequestMapping("/key")
    val key: String?
        get() = rdbDataSourceNotifyListener.key
}
