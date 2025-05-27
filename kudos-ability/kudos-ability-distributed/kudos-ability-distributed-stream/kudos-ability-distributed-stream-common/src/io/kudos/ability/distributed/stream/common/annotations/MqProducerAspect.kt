package io.kudos.ability.distributed.stream.kafka.init

import io.kudos.context.kit.SpringKit
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.soul.ability.distributed.stream.common.support.StreamProducerHelper
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * MQ生产者注解处理
 */
@Aspect
open class MqProducerAspect {

    @Autowired
    private val producerHelper: StreamProducerHelper? = null

    @Pointcut("@annotation(io.kudos.ability.distributed.stream.kafka.init.MqProducer)")
    open fun producerPointcut() {
        println("")
    }

    @AfterReturning(pointcut = "producerPointcut()", returning = "joinPoint")
    open fun afterReturning(joinPoint: JoinPoint) {
        if (joinPoint.args == null || joinPoint.args.size == 0) {
            log.warn("Stream生产消息体为空，忽略本次消息")
            return
        }
        if (producerHelper != null) {
            val signature = joinPoint.signature as MethodSignature
            val annotation = signature.method.getAnnotation(MqProducer::class.java)
            val bindingName: String? = annotation!!.bindingName
            val data = joinPoint.args[0]
            producerHelper.sendMessage(bindingName, data)
        }
    }

    companion object {
        private val log: Log = LogFactory.getLog(MqProducerAspect::class.java)
    }
}
