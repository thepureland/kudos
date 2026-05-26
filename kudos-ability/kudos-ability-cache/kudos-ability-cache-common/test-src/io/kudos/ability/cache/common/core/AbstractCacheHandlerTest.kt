package io.kudos.ability.cache.common.core

import io.kudos.context.kit.SpringKit
import org.springframework.context.support.StaticApplicationContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertSame

/**
 * Unit tests for [AbstractCacheHandler.getSelf] / `selfBeanName()` self-proxy resolution.
 *
 * Verifies the round-2 fix — overriding `selfBeanName()` lets subclasses explicitly specify the bean name
 * in "multiple beans of the same type" scenarios, avoiding [org.springframework.beans.factory.NoUniqueBeanDefinitionException]:
 *
 *  - Default ([AbstractCacheHandler.selfBeanName] returns null) → resolved by type; works with a single bean.
 *  - Multiple beans, resolution by type → throws `NoUniqueBeanDefinitionException` (guards the "you must override" contract).
 *  - Subclass override returns a non-null bean name → resolved by name; works even with multiple beans.
 */
internal class AbstractCacheHandlerTest {

    private lateinit var ctx: StaticApplicationContext

    @BeforeTest
    fun setup() {
        ctx = StaticApplicationContext().apply { refresh() }
        SpringKit.applicationContext = ctx
    }

    @AfterTest
    fun teardown() {
        ctx.close()
    }

    @Test
    fun getSelf_singleBeanByType_resolves() {
        val handler = DefaultHandler()
        ctx.beanFactory.registerSingleton("defaultHandler", handler)

        val resolved: DefaultHandler = handler.exposeSelf()
        assertSame(handler, resolved)
    }

    @Test
    fun getSelf_multiBeansSameTypeNoBeanName_throws() {
        ctx.beanFactory.registerSingleton("h1", DefaultHandler())
        ctx.beanFactory.registerSingleton("h2", DefaultHandler())
        val caller = DefaultHandler()

        // No selfBeanName override → resolution by type → multiple beans should throw.
        assertFails { caller.exposeSelf<DefaultHandler>() }
    }

    @Test
    fun getSelf_multiBeansWithSelfBeanNameOverride_resolves() {
        val h1 = DefaultHandler()
        val h2 = DefaultHandler()
        ctx.beanFactory.registerSingleton("h1", h1)
        ctx.beanFactory.registerSingleton("h2", h2)

        // Subclass declares that it should resolve to "h2".
        val caller = NamedHandler(beanName = "h2")
        val resolved: DefaultHandler = caller.exposeSelf()
        assertSame(h2, resolved)
    }

    private open class DefaultHandler : AbstractCacheHandler<String>() {
        override fun cacheName(): String = "test"
        override fun reloadAll(clear: Boolean) {}
        // Exposes the protected getSelf for tests.
        fun <S : AbstractCacheHandler<*>?> exposeSelf(): S = getSelf()
    }

    private class NamedHandler(private val beanName: String) : DefaultHandler() {
        override fun selfBeanName(): String? = beanName
    }
}
