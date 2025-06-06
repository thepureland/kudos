package io.kudos.ability.distributed.notify.rdb.producer

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.base.logger.LogFactory
import org.soul.ability.distributed.notify.rdb.entity.SysApp
import org.soul.ability.distributed.notify.rdb.service.ISysAppService
import org.soul.base.data.json.JsonTool
import org.soul.base.query.sort.Order
import org.soul.context.ComponentConst
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.scheduling.annotation.Async
import org.springframework.web.client.RestTemplate
import java.io.Serializable
import java.util.function.Consumer

/**
 * @author Fei
 * @date 2023/1/3 9:39
 * @since 5.0.0
 */
open class NotifyRdbProducer : INotifyProducer {

    private val LOG = LogFactory.getLog(this)

    @Autowired
    private lateinit var sysAppService: ISysAppService

    /**
     * 集群节点消息发送
     *
     * @param messageVo
     */
    override fun notify(messageVo: NotifyMessageVo<out Serializable>): Boolean {
        val apps = sysAppService.allSearch(Order.desc(SysApp.PORP_LAST_HEART_TIME))
        sendMessage(apps, messageVo)
        return true
    }

    @Async(value = "appNotifyExecutor")
    open fun sendMessage(apps: List<SysApp>, messageVo: NotifyMessageVo<*>?) {
        val requestHeaders = HttpHeaders()
        requestHeaders.set(ComponentConst.RequestHeader.NOTIFY_REQUEST, "true")
        requestHeaders.set("msgBody", JsonTool.toJson(messageVo))
        val requestEntity = HttpEntity<Any?>(requestHeaders)
        val template = RestTemplate()

        apps.forEach(Consumer { a: SysApp ->
            val uri = "http://${a.ip}:${a.port}${a.appName}/errors/app-notify.html"
            val body = template.postForObject(uri, requestEntity, String::class.java)
            LOG.info("----------> app notify result : $body")
        })
    }

}
