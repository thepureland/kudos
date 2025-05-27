package io.kudos.ability.distributed.notify.mq.main.ms

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.mq.common.NotifyTypeEnum
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.Serializable

@Service
class NotifyMqMsService {
    private val log: Log = LogFactory.getLog(NotifyMqMsService::class.java)

    @Autowired
    private val notifyProducer: INotifyProducer? = null
    private val data: MutableMap<String?, HashSet<Int?>?> = HashMap<String?, HashSet<Int?>?>()

    private val registryMap: MutableMap<Int?, String?> = HashMap<Int?, String?>()

    fun process(key: String?): Boolean {
        val messageVo: NotifyMessageVo<*> = NotifyMessageVo<Any?>()
        messageVo.notifyType = NotifyTypeEnum.DS.getCode()
        messageVo.messageBody = key
        return notifyProducer!!.notify<Serializable?>(messageVo)
    }

    fun collection(port: Int?, appKey: String, key: String?): Boolean {
        var result = false
        if (registryMap.containsKey(port)) {
            if (appKey == registryMap.get(port)) {
                if (!data.containsKey(key)) {
                    data.put(key, HashSet<Int?>())
                }
                if (!data.get(key)!!.contains(port)) {
                    data.get(key)!!.add(port)
                    log.info("@@@ collection, port:{0}, appKey:{1}, key:{2}", port, appKey, key)
                    result = true
                }
            } else log.info(
                "@@@ application appkey not eq, port:{0}, validKey:{1}, appKey:{2}",
                port,
                registryMap.get(port),
                appKey
            )
        } else {
            log.info("@@@ application not match, port:{0}, appKey:{1}", port, appKey)
        }

        return result
    }

    fun isSync(key: String?): Boolean {
        var result = false
        if (data.containsKey(key)) {
            if (data.get(key)!!.size == registryMap.size) result = true
        }
        return result
    }

    fun registry(appKey: String?, port: Int?): Boolean {
        log.info("@@@ registry, port:{0}, appKey:{1}", port, appKey)
        registryMap.put(port, appKey)
        return true
    }
}
