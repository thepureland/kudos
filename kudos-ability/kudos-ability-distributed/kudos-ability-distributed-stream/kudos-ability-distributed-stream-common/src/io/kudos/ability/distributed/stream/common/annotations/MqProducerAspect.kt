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
 * @author AI: Codex
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
        val signature = joinPoint.signature as MethodSignature
        val annotation = signature.method.getAnnotation(MqProducer::class.java)
            ?: run {
                log.warn("MqProducer 注解缺失，method={0}", signature.toShortString())
                return
            }
        if (retVal is Boolean && !retVal && annotation.cancelOnFalse) {
            log.warn("MqProducer 方法返回false，跳过发送。method={0}", joinPoint.signature.toShortString())
            return
        }
        val bindingName = annotation.bindingName
        if (joinPoint.args.size > 1 && annotation.payloadParameterIndex == 0) {
            log.warn(
                "MqProducer 方法有多个参数但未显式指定payloadParameterIndex，默认发送第一个参数。method={0}",
                signature.toShortString()
            )
        }
        val data = selectPayload(joinPoint.args, annotation.payloadParameterIndex)
            ?: run {
                log.warn(
                    "Stream生产消息体为空或payloadParameterIndex越界，忽略本次消息。method={0}, payloadParameterIndex={1}",
                    signature.toShortString(),
                    annotation.payloadParameterIndex
                )
                return
            }
        val success = producerHelper.sendMessage(bindingName, data)
        if (!success) {
            log.warn("Stream生产消息发送结果:false, bindingName={0}", bindingName)
        }
    }

    companion object {
        internal fun selectPayload(args: Array<Any?>, payloadParameterIndex: Int): Any? =
            args.getOrNull(payloadParameterIndex)
    }

}
