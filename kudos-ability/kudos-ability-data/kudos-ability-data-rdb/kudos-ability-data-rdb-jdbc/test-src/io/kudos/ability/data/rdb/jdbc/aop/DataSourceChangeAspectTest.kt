package io.kudos.ability.data.rdb.jdbc.aop

import io.kudos.ability.data.rdb.jdbc.context.DbContext
import io.kudos.ability.data.rdb.jdbc.context.DbParam
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 数据源切换注解切面的上下文恢复测试。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class DataSourceChangeAspectTest {

    @AfterTest
    fun tearDown() {
        DbContext.clear()
    }

    @Test
    fun dsChangeAspect_restoresOuterDbParamAfterNestedCall() {
        DbContext.set(DbParam().apply {
            forcedDs = "outer"
            readonly = false
            enableLog = true
        })

        val result = DsChangeAspect().around(joinPoint(method("dsChanged"), proceed = {
            assertEquals("inner", DbContext.getOrNull()?.forcedDs)
            assertEquals(true, DbContext.getOrNull()?.readonly)
            assertEquals(true, DbContext.getOrNull()?.enableLog)
            "ok"
        }))

        assertEquals("ok", result)
        assertEquals("outer", DbContext.getOrNull()?.forcedDs)
        assertEquals(false, DbContext.getOrNull()?.readonly)
        assertEquals(true, DbContext.getOrNull()?.enableLog)
    }

    @Test
    fun dsChangeAspect_clearsContextWhenNoOuterDbParamExists() {
        val result = DsChangeAspect().around(joinPoint(method("dsChanged"), proceed = {
            assertEquals("inner", DbContext.getOrNull()?.forcedDs)
            "ok"
        }))

        assertEquals("ok", result)
        assertNull(DbContext.getOrNull())
    }

    @Test
    fun tenantDsChangeAspect_restoresOuterDbParamAfterNestedCall() {
        DbContext.set(DbParam().apply {
            forcedDs = "outer"
            readonly = false
        })

        val result = TenantDsChangeAspect().around(joinPoint(method("tenantDsChanged"), proceed = {
            assertEquals("_context::billing", DbContext.getOrNull()?.forcedDs)
            assertEquals(true, DbContext.getOrNull()?.readonly)
            "ok"
        }))

        assertEquals("ok", result)
        assertEquals("outer", DbContext.getOrNull()?.forcedDs)
        assertEquals(false, DbContext.getOrNull()?.readonly)
    }

    @DsChange("inner", readonly = true)
    private fun dsChanged() = Unit

    @TenantDsChange("billing", readonly = true)
    private fun tenantDsChanged() = Unit

    private fun method(name: String): Method = this::class.java.getDeclaredMethod(name)

    private fun joinPoint(method: Method, proceed: () -> Any?): ProceedingJoinPoint {
        val signature = Proxy.newProxyInstance(
            MethodSignature::class.java.classLoader,
            arrayOf(MethodSignature::class.java)
        ) { _, invokedMethod, _ ->
            when (invokedMethod.name) {
                "getMethod" -> method
                "getName" -> method.name
                "getDeclaringType" -> method.declaringClass
                "getDeclaringTypeName" -> method.declaringClass.name
                "toShortString", "toLongString", "toString" -> method.name
                else -> defaultValue(invokedMethod.returnType)
            }
        } as MethodSignature
        return Proxy.newProxyInstance(
            ProceedingJoinPoint::class.java.classLoader,
            arrayOf(ProceedingJoinPoint::class.java)
        ) { _, invokedMethod, _ ->
            when (invokedMethod.name) {
                "proceed" -> proceed()
                "getSignature" -> signature
                "getArgs" -> emptyArray<Any>()
                "getTarget", "getThis" -> this
                "toShortString", "toLongString", "toString" -> method.name
                else -> defaultValue(invokedMethod.returnType)
            }
        } as ProceedingJoinPoint
    }

    private fun defaultValue(returnType: Class<*>): Any? {
        return when (returnType) {
            java.lang.Boolean.TYPE -> false
            java.lang.Byte.TYPE -> 0.toByte()
            java.lang.Short.TYPE -> 0.toShort()
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            java.lang.Double.TYPE -> 0.0
            java.lang.Character.TYPE -> 0.toChar()
            java.lang.Void.TYPE -> Unit
            else -> null
        }
    }
}
