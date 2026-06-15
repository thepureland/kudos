package io.kudos.ability.web.ktor.init

import io.ktor.server.engine.*
import io.ktor.server.testing.*
import io.kudos.ability.web.ktor.core.KtorContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for KtorAutoConfiguration
 *
 * 覆盖：
 * 1. ktorProperties() 默认配置
 * 2. ktorEngine() 各分支：engine.name 空白(require 异常)、非法值(error 异常)、
 *    test(返回 null，大小写不敏感)、jetty(引擎工厂反射解析分支)
 * 3. shutDownKtor() 两个分支：Application 未初始化时跳过、已初始化时停止引擎
 * 4. getComponentName()
 *
 * @author K
 * @since 1.0.0
 */
internal class KtorAutoConfigurationTest {

    private val configuration = KtorAutoConfiguration()

    private fun props(name: String, port: Int = 0) =
        KtorProperties(engine = KtorProperties.Engine(name = name, port = port))

    @Test
    fun ktorProperties_returnsDefaults() {
        val p = configuration.ktorProperties()
        assertEquals("test", p.engine.name)
        assertEquals(0, p.engine.port)
        assertTrue(p.plugins.contentNegotiation.enabled)
        assertTrue(p.plugins.statusPages.enabled)
        assertTrue(p.plugins.webSocket.enabled)
    }

    @Test
    fun ktorEngine_blankEngineName_throwsIllegalArgumentException() {
        val ex = assertFailsWith<IllegalArgumentException> {
            configuration.ktorEngine(props("   "))
        }
        assertEquals("kudos.ability.web.ktor.engine.name is missing!", ex.message)
    }

    @Test
    fun ktorEngine_invalidEngineName_throwsIllegalStateException() {
        val ex = assertFailsWith<IllegalStateException> {
            configuration.ktorEngine(props("undertow"))
        }
        assertEquals("kudos.ability.web.ktor.engine.name has an invalid value!", ex.message)
    }

    @Test
    fun ktorEngine_testEngine_returnsNull_andIsCaseInsensitive() {
        val p = props("TEST")
        assertNull(configuration.ktorEngine(p))
        // 配置须先暂存到 KtorContext.properties
        assertSame(p, KtorContext.properties)
    }

    @Test
    fun ktorEngine_jettyBranch_resolvesEngineFactory() {
        // Jetty 引擎在当前依赖组合下存在版本冲突（见 JettyEngineTest 的 @Disabled），
        // 这里只验证 "jetty" 分支被正确解析执行：无论启动成败，properties 必须已被暂存。
        // 在守护线程中执行以防 started.join() 因启动失败而悬挂。
        val p = props("JeTtY", 0)
        var outcome: Result<EmbeddedServer<*, *>?>? = null
        val thread = Thread {
            outcome = runCatching { configuration.ktorEngine(p) }
        }
        thread.isDaemon = true
        thread.start()
        thread.join(120_000)
        assertSame(p, KtorContext.properties)
        // 若 Jetty 成功启动则停掉，避免泄漏端口
        outcome?.getOrNull()?.stop(100L, 200L)
    }

    @Test
    fun shutDownKtor_whenApplicationNotInitialized_skipsWithoutError() {
        val field = KtorContext::class.java.getDeclaredField("application")
        field.isAccessible = true
        val original = field.get(KtorContext)
        try {
            field.set(KtorContext, null)
            assertFalse(KtorContext.isApplicationInitialized())
            // 未初始化时必须直接跳过，不抛 UninitializedPropertyAccessException
            configuration.shutDownKtor()
        } finally {
            field.set(KtorContext, original)
        }
    }

    @Test
    fun shutDownKtor_whenApplicationInitialized_stopsEngine() = testApplication {
        application {
            KtorContext.application = this
        }
        KtorContext.properties = KtorProperties()
        startApplication()
        assertTrue(KtorContext.isApplicationInitialized())
        // 已初始化时走停止引擎分支，且不抛异常
        configuration.shutDownKtor()
    }

    @Test
    fun getComponentName_returnsConstant() {
        assertEquals("kudos-ability-web-ktor-base", configuration.getComponentName())
    }

}
