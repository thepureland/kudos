package io.kudos.ability.distributed.notify.rdb.ms.common

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class NotifyRdbMsController {
    @Autowired
    private val notifyMqMsService: NotifyRdbMsService? = null

    @Autowired
    private val rdbDataSourceNotifyListener: RdbDataSourceNotifyListener? = null

    @RequestMapping("/change")
    fun change(@RequestParam("key") key: String?): Boolean {
        return notifyMqMsService!!.process(key)
    }

    @get:RequestMapping("/key")
    val key: String?
        get() = rdbDataSourceNotifyListener!!.getKey()
}
