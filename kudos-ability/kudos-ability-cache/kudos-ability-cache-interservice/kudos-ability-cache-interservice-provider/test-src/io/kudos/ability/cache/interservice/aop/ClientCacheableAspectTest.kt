package io.kudos.ability.cache.interservice.aop

import org.aspectj.lang.ProceedingJoinPoint
import org.springframework.web.bind.annotation.RestController
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertFailsWith

internal class ClientCacheableAspectTest {

    @Test
    fun around_rethrowsOriginalException() {
        val cause = IllegalStateException("boom")
        val aspect = ClientCacheableAspect()

        val thrown = assertFailsWith<IllegalStateException> {
            aspect.around(joinPoint(Target::class.java.getDeclaredMethod("fail")) { throw cause })
        }

        assertSame(cause, thrown)
    }

    companion object {
        private val target = Target()

        private fun joinPoint(targetMethod: Method, proceed: () -> Any?): ProceedingJoinPoint =
            proxy(ProceedingJoinPoint::class.java) { joinPointMethod, _ ->
                when (joinPointMethod.name) {
                    "getTarget" -> target
                    "proceed" -> proceed()
                    else -> defaultValue(joinPointMethod.returnType)
                }
            }

        private fun <T> proxy(type: Class<T>, handler: (Method, Array<Any?>?) -> Any?): T =
            type.cast(
                Proxy.newProxyInstance(
                    type.classLoader,
                    arrayOf(type),
                    InvocationHandler { _, method, args -> handler(method, args) }
                )
            )

        private fun defaultValue(returnType: Class<*>): Any? =
            when (returnType) {
                java.lang.Boolean.TYPE -> false
                java.lang.Byte.TYPE -> 0.toByte()
                java.lang.Short.TYPE -> 0.toShort()
                java.lang.Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Float.TYPE -> 0f
                java.lang.Double.TYPE -> 0.0
                java.lang.Character.TYPE -> 0.toChar()
                java.lang.Void.TYPE -> null
                else -> null
            }
    }

    @RestController
    private class Target {
        fun fail(): String = "unused"
    }
}
