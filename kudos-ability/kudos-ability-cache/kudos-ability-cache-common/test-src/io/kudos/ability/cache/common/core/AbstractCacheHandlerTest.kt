package io.kudos.ability.cache.common.core

import io.kudos.context.kit.SpringKit
import org.springframework.context.support.StaticApplicationContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertSame

/**
 * [AbstractCacheHandler.getSelf] / `selfBeanName()` 自代理解析单测。
 *
 * 验证 round-2 修复——`selfBeanName()` override 允许子类在"同型多 bean"场景下显式指定
 * bean 名，避开 [org.springframework.beans.factory.NoUniqueBeanDefinitionException]：
 *
 *  - 默认（[AbstractCacheHandler.selfBeanName] 返回 null）→ 按类型解析；单 bean 时正常拿到
 *  - 多 bean 时按类型解析 → 抛 `NoUniqueBeanDefinitionException`（守住"必须 override 才行"的契约）
 *  - 子类 override 返回非空 bean 名 → 按名解析，多 bean 也能命中
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

        // 没有 override selfBeanName → 按类型查 → 多 bean 应抛
        assertFails { caller.exposeSelf<DefaultHandler>() }
    }

    @Test
    fun getSelf_multiBeansWithSelfBeanNameOverride_resolves() {
        val h1 = DefaultHandler()
        val h2 = DefaultHandler()
        ctx.beanFactory.registerSingleton("h1", h1)
        ctx.beanFactory.registerSingleton("h2", h2)

        // 子类指明自己应当解析为 "h2"
        val caller = NamedHandler(beanName = "h2")
        val resolved: DefaultHandler = caller.exposeSelf()
        assertSame(h2, resolved)
    }

    private open class DefaultHandler : AbstractCacheHandler<String>() {
        override fun cacheName(): String = "test"
        override fun reloadAll(clear: Boolean) {}
        // 暴露 protected getSelf 给测试调用
        fun <S : AbstractCacheHandler<*>?> exposeSelf(): S = getSelf()
    }

    private class NamedHandler(private val beanName: String) : DefaultHandler() {
        override fun selfBeanName(): String? = beanName
    }
}
