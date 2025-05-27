package io.kudos.ability.distributed.notify.rdb.producer

import org.soul.ability.distributed.notify.common.api.INotifyProducer
import org.soul.ability.distributed.notify.common.model.NotifyMessageVo
import org.soul.ability.distributed.notify.rdb.entity.SysApp
import org.soul.ability.distributed.notify.rdb.service.ISysAppService
import org.soul.base.data.json.JsonTool
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.soul.base.query.sort.Order
import org.soul.context.ComponentConst
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.Serializable
import java.util.function.Consumer

/**
 * @author Fei
 * @date 2023/1/3 9:39
 * @since 5.0.0
 */
@Service
class NotifyRdbProducer : INotifyProducer {
    @Autowired
    var sysAppService: ISysAppService? = null

    /**
     * 集群节点消息发送
     *
     * @param messageVo
     */
    override fun <T : Serializable?> notify(messageVo: NotifyMessageVo<T?>?): Boolean {
        val apps = sysAppService!!.allSearch(Order.desc(SysApp.PORP_LAST_HEART_TIME))
        sendMessage(apps, messageVo)
        return true
    }

    @Async(value = "appNotifyExecutor")
    fun sendMessage(apps: MutableList<SysApp?>, messageVo: NotifyMessageVo<*>?) {
        val requestHeaders = HttpHeaders()
        requestHeaders.set(ComponentConst.RequestHeader.NOTIFY_REQUEST, "true")
        requestHeaders.set("msgBody", JsonTool.toJson(messageVo))
        val requestEntity: HttpEntity<*> = HttpEntity<Any?>(requestHeaders)
        val template = RestTemplate()

        apps.forEach(Consumer { a: SysApp? ->
            val uri = "http://" + a!!.getIp() + ":" + a.getPort() + a.getAppName() + "/errors/app-notify.html"
            val body = template.postForObject<String?>(uri, requestEntity, String::class.java)
            LOG.info("----------> app notify result : {0}", body)
        })
    }

    companion object {
        private val LOG: Log = LogFactory.getLog(NotifyRdbProducer::class.java)
    }
}
