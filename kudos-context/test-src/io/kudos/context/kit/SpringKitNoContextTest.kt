package io.kudos.context.kit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SpringKit's "context not initialized" guard, plus the bean lookup overloads
 * against a hand-built StaticApplicationContext (no Spring Boot container required).
 *
 * Coverage:
 * - The throwing getter of [SpringKit.applicationContext] when nothing was injected
 * - getBeanOrNull(name) / getBeanOrNull(KClass): both hit and miss paths
 * - getBean(KClass) hit path, getProperty miss path
 *
 * Each test saves and restores the global [SpringKit.applicationContext] so it never
 * pollutes Spring Boot test classes running in the same JVM.
 *
 * @author K
 * @since 1.0.0
 */
internal class SpringKitNoContextTest {

    private fun withSavedContext(block: () -> Unit) {
        val prev = runCatching { SpringKit.applicationContext }.getOrNull()
        try {
            block()
        } finally {
            SpringKit.applicationContext = prev
        }
    }

    @Test
    fun applicationContextGetterThrowsWhenNotInitialized() = withSavedContext {
        SpringKit.applicationContext = null
        val e = assertFailsWith<IllegalStateException> { SpringKit.applicationContext }
        assertTrue(
            e.message!!.contains("not initialized"),
            "the error message should explain the context is missing: ${e.message}"
        )
    }

    @Test
    fun reifiedGetBeanEmittedCopyRejectsNonInlinedInvocation() {
        // `inline fun <reified T : Any> getBean(): T` keeps a non-inlined copy in the class file for
        // binary compatibility; per the Kotlin spec that copy must refuse to run (reified type
        // parameters only exist at inline sites). Pin that contract down reflectively.
        val emitted = SpringKit::class.java.declaredMethods
            .first { it.name == "getBean" && it.parameterCount == 0 }
            .apply { isAccessible = true }
        val e = assertFailsWith<java.lang.reflect.InvocationTargetException> { emitted.invoke(SpringKit) }
        assertTrue(
            e.cause is UnsupportedOperationException,
            "the reified copy must throw UnsupportedOperationException, got: ${e.cause}"
        )
    }

    @Test
    fun beanLookupsWorkAgainstStaticContext() = withSavedContext {
        val ctx = org.springframework.context.support.StaticApplicationContext()
        ctx.refresh()
        val marker = StringBuilder("marker")
        ctx.beanFactory.registerSingleton("markerBean", marker)
        SpringKit.applicationContext = ctx

        // by name
        assertEquals(marker, SpringKit.getBean("markerBean"))
        assertEquals(marker, SpringKit.getBeanOrNull("markerBean"))
        assertNull(SpringKit.getBeanOrNull("noSuchBean"), "a missing bean name should yield null instead of throwing")

        // by class
        assertEquals(marker, SpringKit.getBean(StringBuilder::class))
        assertEquals(marker, SpringKit.getBeanOrNull(StringBuilder::class))
        assertNull(SpringKit.getBeanOrNull(java.math.BigDecimal::class), "a missing bean type should yield null")

        // beans of type
        assertEquals(mapOf("markerBean" to marker), SpringKit.getBeansOfType(StringBuilder::class))

        // property miss
        assertNull(SpringKit.getProperty("kudos.no.such.property"))
        ctx.close()
    }
}
