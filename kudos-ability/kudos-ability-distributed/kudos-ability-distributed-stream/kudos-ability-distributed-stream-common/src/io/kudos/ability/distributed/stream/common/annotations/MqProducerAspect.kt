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
 * Aspect for the MQ producer annotation.
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
                log.warn("MqProducer annotation missing, method={0}", signature.toShortString())
                return
            }
        if (retVal is Boolean && !retVal && annotation.cancelOnFalse) {
            log.warn("MqProducer method returned false, skipping send. method={0}", joinPoint.signature.toShortString())
            return
        }
        val bindingName = annotation.bindingName
        if (joinPoint.args.size > 1 && annotation.payloadParameterIndex == 0) {
            log.warn(
                "MqProducer method has multiple parameters but payloadParameterIndex is not set explicitly; sending the first parameter by default. method={0}",
                signature.toShortString()
            )
        }
        val data = selectPayload(joinPoint.args, annotation.payloadParameterIndex)
            ?: run {
                log.warn(
                    "Stream producer payload is null or payloadParameterIndex out of range; ignoring this message. method={0}, payloadParameterIndex={1}",
                    signature.toShortString(),
                    annotation.payloadParameterIndex
                )
                return
            }
        val success = producerHelper.sendMessage(bindingName, data)
        if (!success) {
            log.warn("Stream producer send result:false, bindingName={0}", bindingName)
        }
    }

    companion object {
        internal fun selectPayload(args: Array<Any?>, payloadParameterIndex: Int): Any? =
            args.getOrNull(payloadParameterIndex)
    }

}
