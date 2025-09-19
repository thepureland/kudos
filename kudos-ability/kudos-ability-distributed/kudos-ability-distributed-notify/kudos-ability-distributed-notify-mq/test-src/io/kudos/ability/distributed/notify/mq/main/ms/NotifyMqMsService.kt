package io.kudos.ability.distributed.notify.mq.main.ms

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.mq.common.NotifyTypeEnum
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service

@Service
open class NotifyMqMsService {

    private val log = LogFactory.getLog(this)

    @Autowired
    private val notifyProducer: INotifyProducer? = null
    private val data: MutableMap<String?, HashSet<Int?>?> = HashMap()

    private val registryMap: MutableMap<Int?, String?> = HashMap()

//    @Bean
//    fun contextParam(): ContextParam {
//        val context = ContextParam()
//        // 这个数据类型目前是Integer类型, 像配置文件定义的db1,db2这种字符串会有问题
//        // jdbc模块也有相同问题, 后续建议统一调整为Object类型,并自动对String,Integer,Long等常用类型进行转换
//        context.username = "notifyMqTestUser"
//        CommonContext.set(context)
//        return context
//    }

    fun process(key: String): Boolean {
        val messageVo = NotifyMessageVo<String>()
        messageVo.notifyType = NotifyTypeEnum.DS.code
        messageVo.messageBody = key
        return notifyProducer!!.notify(messageVo)
    }

    fun collection(port: Int?, appKey: String, key: String?): Boolean {
        var result = false
        if (registryMap.containsKey(port)) {
            if (appKey == registryMap[port]) {
                if (!data.containsKey(key)) {
                    data.put(key, HashSet<Int?>())
                }
                if (!data[key]!!.contains(port)) {
                    data[key]!!.add(port)
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
        var result = false
        if (data.containsKey(key)) {
            if (data[key]!!.size == registryMap.size) result = true
        }
        return result
    }

    fun registry(appKey: String?, port: Int?): Boolean {
        log.info("@@@ registry, port:{0}, appKey:{1}", port, appKey)
        registryMap.put(port, appKey)
        return true
    }
}
