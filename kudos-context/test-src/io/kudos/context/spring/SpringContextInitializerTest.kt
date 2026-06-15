package io.kudos.context.spring

import io.kudos.context.kit.SpringKit
import org.springframework.context.support.StaticApplicationContext
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * test for SpringContextInitializer
 *
 * Coverage: initialize() publishes the given context into [SpringKit.applicationContext].
 * The previous global context is saved and restored so this test cannot pollute Spring Boot
 * tests sharing the JVM.
 *
 * @author K
 * @since 1.0.0
 */
internal class SpringContextInitializerTest {

    @Test
    fun initializePublishesContextIntoSpringKit() {
        val prev = runCatching { SpringKit.applicationContext }.getOrNull()
        val ctx = StaticApplicationContext()
        try {
            SpringContextInitializer().initialize(ctx)
            assertSame(ctx, SpringKit.applicationContext)
        } finally {
            SpringKit.applicationContext = prev
            ctx.close()
        }
    }
}
