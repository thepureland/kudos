package io.kudos.ability.distributed.notify.mq.main.ms

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.mq.common.NotifyTypeEnum
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
open class NotifyMqMsService {

    private val log = LogFactory.getLog(this::class)

    @Autowired
    private val notifyProducer: INotifyProducer? = null
    private val data: MutableMap<String?, HashSet<Int?>> = HashMap()

    private val registryMap: MutableMap<Int?, String?> = HashMap()

//    @Bean
//    fun contextParam(): ContextParam {
//        val context = ContextParam()
//        // This data type is currently Integer; strings defined in config files like db1, db2 will cause issues.
//        // The jdbc module has the same problem; recommend switching to Object type and auto-converting common types like String, Integer, Long.
//        context.username = "notifyMqTestUser"
//        CommonContext.set(context)
//        return context
//    }

    fun process(key: String): Boolean {
        val messageVo = NotifyMessageVo<String>()
        messageVo.notifyType = NotifyTypeEnum.DS.code
        messageVo.messageBody = key
        return requireNotNull(notifyProducer) { "INotifyProducer is not injected" }.notify(messageVo)
    }

    fun collection(port: Int?, appKey: String, key: String?): Boolean {
        var result = false
        if (registryMap.containsKey(port)) {
            if (appKey == registryMap[port]) {
                val ports = data.getOrPut(key) { HashSet() }
                if (!ports.contains(port)) {
                    ports.add(port)
                    log.info("@@@ collection, port:{0}, appKey:{1}, key:{2}", port, appKey, key)
                    result = true
                }
            } else log.info(
                "@@@ application appkey not eq, port:{0}, validKey:{1}, appKey:{2}",
                port,
                registryMap[port],
                appKey
            )
        } else {
            log.info("@@@ application not match, port:{0}, appKey:{1}", port, appKey)
        }

        return result
    }

    fun isSync(key: String?): Boolean {
        val ports = data[key] ?: return false
        return ports.size == registryMap.size
    }

    fun registry(appKey: String?, port: Int?): Boolean {
        log.info("@@@ registry, port:{0}, appKey:{1}", port, appKey)
        registryMap[port] = appKey
        return true
    }
}
