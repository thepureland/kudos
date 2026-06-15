package io.kudos.ability.distributed.stream.common.annotations

import io.kudos.ability.distributed.stream.common.support.StreamProducerHelper
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [MqProducerAspect]:
 * - payload parameter selection ([MqProducerAspect.selectPayload]) including out-of-range index
 * - [MqProducerAspect.afterReturning] routing branches: missing annotation, Boolean false with
 *   cancelOnFalse on/off, multi-parameter default-index warning, null payload skip, send
 *   success / failure logging
 * - annotation default attribute values of [MqProducer] and [MqConsumer]
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
internal class MqProducerAspectTest {

    @Test
    fun selectPayload_usesConfiguredParameterIndex() {
        val args = arrayOf<Any?>("ignored", TestPayload("selected"))

        assertEquals(TestPayload("selected"), MqProducerAspect.selectPayload(args, 1))
    }

    @Test
    fun selectPayload_returnsNullWhenIndexIsOutOfBounds() {
        val args = arrayOf<Any?>("only")

        assertNull(MqProducerAspect.selectPayload(args, 2))
    }

    @Test
    fun producerPointcut_isCallableNoOp() {
        // The pointcut method body is empty; calling it just keeps the line covered.
        MqProducerAspect().producerPointcut()
    }

    @Test
    fun afterReturning_skipsWhenAnnotationIsMissing() {
        val (aspect, helper) = newAspect()
        val joinPoint = joinPointFor("plain", arrayOf("data"))

        aspect.afterReturning(joinPoint, "ret")

        verifyNoInteractions(helper)
    }

    @Test
    fun afterReturning_skipsSendWhenMethodReturnsFalseAndCancelOnFalseIsDefault() {
        val (aspect, helper) = newAspect()
        val joinPoint = joinPointFor("single", arrayOf("data"))

        aspect.afterReturning(joinPoint, false)

        verifyNoInteractions(helper)
    }

    @Test
    fun afterReturning_sendsWhenMethodReturnsFalseButCancelOnFalseDisabled() {
        val (aspect, helper) = newAspect(sendResult = true)
        val joinPoint = joinPointFor("noCancel", arrayOf("data"))

        aspect.afterReturning(joinPoint, false)

        verify(helper).sendMessage("bind-a", "data")
    }

    @Test
    fun afterReturning_sendsPayloadWhenMethodReturnsTrue() {
        val (aspect, helper) = newAspect(sendResult = true)
        val joinPoint = joinPointFor("single", arrayOf("data"))

        aspect.afterReturning(joinPoint, true)

        verify(helper).sendMessage("bind-a", "data")
    }

    @Test
    fun afterReturning_warnsAndSendsFirstArgWhenMultipleParamsWithDefaultIndex() {
        val (aspect, helper) = newAspect(sendResult = true)
        val joinPoint = joinPointFor("multi", arrayOf("first", "second"))

        aspect.afterReturning(joinPoint, null)

        verify(helper).sendMessage("bind-a", "first")
    }

    @Test
    fun afterReturning_sendsConfiguredParameterForMultiParamMethod() {
        val (aspect, helper) = newAspect(sendResult = true)
        val joinPoint = joinPointFor("multiIndexed", arrayOf("first", "second"))

        aspect.afterReturning(joinPoint, null)

        verify(helper).sendMessage("bind-a", "second")
    }

    @Test
    fun afterReturning_skipsSendWhenPayloadIsNull() {
        val (aspect, helper) = newAspect()
        val joinPoint = joinPointFor("single", arrayOf(null))

        aspect.afterReturning(joinPoint, "ok")

        verifyNoInteractions(helper)
    }

    @Test
    fun afterReturning_logsWarningWhenSendFails() {
        val (aspect, helper) = newAspect(sendResult = false)
        val joinPoint = joinPointFor("single", arrayOf("data"))

        // Must not throw even though the helper reports failure.
        aspect.afterReturning(joinPoint, "ok")

        verify(helper).sendMessage("bind-a", "data")
    }

    @Test
    fun mqProducerAnnotation_hasDocumentedDefaults() {
        val annotation = Fixture::class.java
            .getDeclaredMethod("single", String::class.java)
            .getAnnotation(MqProducer::class.java)!!

        assertEquals("bind-a", annotation.bindingName)
        assertEquals("", annotation.topic)
        assertEquals(0, annotation.payloadParameterIndex)
        assertEquals(true, annotation.cancelOnFalse)
    }

    @Test
    fun mqConsumerAnnotation_hasDocumentedDefaults() {
        val annotation = Fixture::class.java
            .getDeclaredMethod("consumer")
            .getAnnotation(MqConsumer::class.java)!!

        assertEquals(0, annotation.beanName.size)
        assertEquals("", annotation.bindingName)
        assertEquals("", annotation.topic)
    }

    private fun newAspect(sendResult: Boolean = true): Pair<MqProducerAspect, StreamProducerHelper> {
        val aspect = MqProducerAspect()
        val helper = mock(StreamProducerHelper::class.java)
        `when`(helper.sendMessage<Any>(anyString(), any<Any>())).thenReturn(sendResult)
        val field = MqProducerAspect::class.java.getDeclaredField("producerHelper")
        field.isAccessible = true
        field.set(aspect, helper)
        return aspect to helper
    }

    private fun joinPointFor(methodName: String, args: Array<Any?>): JoinPoint {
        val method = Fixture::class.java.declaredMethods.single { it.name == methodName }
        val signature = mock(MethodSignature::class.java)
        `when`(signature.method).thenReturn(method)
        `when`(signature.toShortString()).thenReturn("Fixture.$methodName(..)")
        val joinPoint = mock(JoinPoint::class.java)
        `when`(joinPoint.signature).thenReturn(signature)
        `when`(joinPoint.args).thenReturn(args)
        return joinPoint
    }

    /**
     * Fixture providing annotated producer methods for the aspect tests.
     *
     * @author K
     * @author AI: Claude
     * @since 1.0.0
     */
    @Suppress("unused", "UNUSED_PARAMETER")
    private class Fixture {
        @MqProducer(bindingName = "bind-a")
        fun single(data: String): Boolean = true

        @MqProducer(bindingName = "bind-a", cancelOnFalse = false)
        fun noCancel(data: String): Boolean = false

        @MqProducer(bindingName = "bind-a")
        fun multi(first: String, second: String) {
        }

        @MqProducer(bindingName = "bind-a", payloadParameterIndex = 1)
        fun multiIndexed(first: String, second: String) {
        }

        fun plain(data: String) {
        }

        @MqConsumer
        fun consumer(): java.util.function.Consumer<String> = java.util.function.Consumer { }
    }

    /**
     * Test DTO for producer payload selection.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private data class TestPayload(val value: String)
}
