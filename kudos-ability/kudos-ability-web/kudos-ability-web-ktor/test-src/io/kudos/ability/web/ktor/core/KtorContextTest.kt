package io.kudos.ability.web.ktor.core

import io.ktor.server.testing.*
import io.kudos.ability.web.ktor.init.KtorProperties
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for KtorContext
 *
 * 覆盖：
 * 1. isApplicationInitialized() 的 false 分支（lateinit 未赋值）与 true 分支（赋值后）
 * 2. properties 的读写
 *
 * @author K
 * @since 1.0.0
 */
internal class KtorContextTest {

    @Test
    fun isApplicationInitialized_falseBeforeAssignment_trueAfter() = testApplication {
        val field = KtorContext::class.java.getDeclaredField("application")
        field.isAccessible = true
        val original = field.get(KtorContext)
        try {
            field.set(KtorContext, null)
            assertFalse(KtorContext.isApplicationInitialized())

            application { KtorContext.application = this }
            startApplication()

            assertTrue(KtorContext.isApplicationInitialized())
            assertNotNull(KtorContext.application)
        } finally {
            field.set(KtorContext, original)
        }
    }

    @Test
    fun properties_canBeAssignedAndRead() {
        val p = KtorProperties()
        KtorContext.properties = p
        assertSame(p, KtorContext.properties)
    }

}
