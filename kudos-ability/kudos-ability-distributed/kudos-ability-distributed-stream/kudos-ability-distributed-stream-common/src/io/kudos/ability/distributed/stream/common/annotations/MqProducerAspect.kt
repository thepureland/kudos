package io.kudos.ability.distributed.stream.common.annotations

import io.kudos.ability.distributed.stream.common.support.StreamProducerHelper
import io.kudos.base.logger.LogFactory
import jakarta.annotation.Resource
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature

/**
 * MQ生产者注解切面
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
@Aspect
open class MqProducerAspect {

    @Resource
    private lateinit var producerHelper: StreamProducerHelper

    private val log = LogFactory.getLog(this)

    @Pointcut("@annotation(io.kudos.ability.distributed.stream.common.annotations.MqProducer)")
    open fun producerPointcut() {
    }

    @AfterReturning(pointcut = "producerPointcut()", returning = "joinPoint")
    open fun afterReturning(joinPoint: JoinPoint) {
        if (joinPoint.args == null || joinPoint.args.size == 0) {
            log.warn("Stream生产消息体为空，忽略本次消息")
            return
        }
        val signature = joinPoint.signature as MethodSignature
        val annotation = signature.method.getAnnotation(MqProducer::class.java)
        val bindingName = annotation!!.bindingName
        val data = joinPoint.args[0]
        producerHelper.sendMessage(bindingName, data)
    }

}
