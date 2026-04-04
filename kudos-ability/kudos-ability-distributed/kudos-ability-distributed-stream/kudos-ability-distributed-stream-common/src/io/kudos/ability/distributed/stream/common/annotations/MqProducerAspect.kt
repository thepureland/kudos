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

    private val log = LogFactory.getLog(this::class)

    @Pointcut("@annotation(io.kudos.ability.distributed.stream.common.annotations.MqProducer)")
    open fun producerPointcut() {
    }

    @AfterReturning(pointcut = "producerPointcut()", returning = "retVal")
    open fun afterReturning(joinPoint: JoinPoint, retVal: Any?) {
        if (retVal is Boolean && !retVal) {
            log.warn("MqProducer 方法返回false，跳过发送。method={0}", joinPoint.signature.toShortString())
            return
        }
        if (joinPoint.args.isEmpty()) {
            log.warn("Stream生产消息体为空，忽略本次消息")
            return
        }
        val signature = joinPoint.signature as MethodSignature
        val annotation = signature.method.getAnnotation(MqProducer::class.java)
            ?: run {
                log.warn("MqProducer 注解缺失，method={0}", signature.toShortString())
                return
            }
        val bindingName = annotation.bindingName
        val data = joinPoint.args[0]
        val success = producerHelper.sendMessage(bindingName, data)
        if (!success) {
            log.warn("Stream生产消息发送结果:false, bindingName={0}", bindingName)
        }
    }

}
