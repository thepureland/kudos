package io.kudos.test.common.init

import io.kudos.context.kit.SpringKit
import org.mockito.Mockito
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * Unit tests for [SpringKitTestContextListener].
 *
 * Verifies that [SpringKitTestContextListener.prepareTestInstance] copies the current test's
 * [ApplicationContext] into the static [SpringKit.applicationContext], which is the whole point of the listener
 * (re-syncing SpringKit when the Spring test framework reuses a cached context). [TestContext] and
 * [ApplicationContext] are mocked via Mockito; no real Spring container is started.
 *
 * @author K
 * @since 1.0.0
 */
internal class SpringKitTestContextListenerTest {

    @AfterTest
    fun restore() {
        // Avoid leaking a mock context into other tests sharing the same JVM static.
        SpringKit.applicationContext = null
    }

    @Test
    fun prepareTestInstanceSyncsApplicationContext() {
        val ctx = Mockito.mock(ApplicationContext::class.java)
        val testContext = Mockito.mock(TestContext::class.java)
        Mockito.`when`(testContext.applicationContext).thenReturn(ctx)

        val listener = SpringKitTestContextListener()
        listener.prepareTestInstance(testContext)

        assertSame(ctx, SpringKit.applicationContext)
    }

    @Test
    fun prepareTestInstanceOverwritesPreviousContext() {
        val first = Mockito.mock(ApplicationContext::class.java)
        val second = Mockito.mock(ApplicationContext::class.java)
        SpringKit.applicationContext = first

        val testContext = Mockito.mock(TestContext::class.java)
        Mockito.`when`(testContext.applicationContext).thenReturn(second)

        SpringKitTestContextListener().prepareTestInstance(testContext)

        assertSame(second, SpringKit.applicationContext)
    }
}
