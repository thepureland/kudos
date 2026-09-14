package io.kudos.test.common.init

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import org.mockito.Mockito
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Unit tests for [SpringKitTestContextListener].
 *
 * Verifies that [SpringKitTestContextListener.prepareTestInstance] clears thread state left by a preceding test
 * context and copies the current test's [ApplicationContext] into the static [SpringKit.applicationContext].
 * [TestContext] and [ApplicationContext] are mocked via Mockito; no real Spring container is started.
 *
 * @author K
 * @since 1.0.0
 */
internal class SpringKitTestContextListenerTest {

    @AfterTest
    fun restore() {
        // Avoid leaking a mock context into other tests sharing the same JVM static.
        SpringKit.applicationContext = null
        KudosContextHolder.clear()
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

    @Test
    fun prepareTestInstanceClearsThreadContextBoundToPreviousApplicationContext() {
        val staleDataSource = Any()
        KudosContextHolder.get().addOtherInfos(KudosContext.OTHER_INFO_KEY_DATA_SOURCE to staleDataSource)

        val nextApplicationContext = Mockito.mock(ApplicationContext::class.java)
        val testContext = Mockito.mock(TestContext::class.java)
        Mockito.`when`(testContext.applicationContext).thenReturn(nextApplicationContext)

        SpringKitTestContextListener().prepareTestInstance(testContext)

        assertNull(KudosContextHolder.getOrNull())
    }
}
