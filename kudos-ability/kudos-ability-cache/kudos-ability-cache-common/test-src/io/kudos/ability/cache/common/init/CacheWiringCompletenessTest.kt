package io.kudos.ability.cache.common.init

import io.kudos.ability.cache.common.aop.keyvalue.TenantCacheEvict
import io.kudos.ability.cache.common.aop.keyvalue.TenantCachePut
import io.kudos.ability.cache.common.aop.keyvalue.TenantCacheable
import org.aspectj.lang.annotation.Aspect
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import java.lang.reflect.Method
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Structural guard over this module's Spring wiring.
 *
 * Kudos assembles components by importing [io.kudos.context.init.IComponentInitializer] configuration classes
 * (see `ComponentInitializerSelector`); it never component-scans `io.kudos.ability.**`. A `@Component` on an
 * aspect therefore does nothing, and an aspect that is not also declared as an `@Bean` is simply never applied
 * — the annotation it backs silently does nothing at runtime.
 *
 * That failure mode has bitten this module more than once: `DistributedCacheGuardAspect` shipped unregistered,
 * and later `TenantCachingAspect` / `TenantAdvancedCacheableAspect` / `TenantAdvancedCacheEvictAspect` did too,
 * alongside the `tenantCacheKeyGenerator` bean that the `@Tenant*` annotations name in their defaults. None of
 * it was caught, because the existing tests call the `@Bean` factory methods directly — which passes whether or
 * not the method is ever declared.
 *
 * These tests assert on the *declarations* instead, so a newly added aspect or key generator that nobody wires
 * fails here rather than in production. They deliberately avoid booting a context: the real one needs Redis /
 * Caffeine infrastructure, and the bug class being guarded is "nothing declares this bean", which is visible
 * statically.
 *
 * @author K
 * @since 1.0.0
 */
internal class CacheWiringCompletenessTest {

    private companion object {
        const val BASE_PACKAGE = "io.kudos.ability.cache.common"

        /** Bean names declared across the module's configuration classes. */
        val declaredBeanNames: Set<String> =
            beanMethodsOf(LinkableCacheAutoConfiguration::class.java)
                .plus(beanMethodsOf(BaseCacheConfiguration::class.java))
                .flatMap { beanNamesOf(it) }
                .toSet()

        /** Types produced by the module's `@Bean` factory methods. */
        val declaredBeanTypes: Set<Class<*>> =
            beanMethodsOf(LinkableCacheAutoConfiguration::class.java)
                .plus(beanMethodsOf(BaseCacheConfiguration::class.java))
                .map { it.returnType }
                .toSet()

        fun beanMethodsOf(configClass: Class<*>): List<Method> =
            configClass.declaredMethods.filter { it.isAnnotationPresent(Bean::class.java) } +
                // Factory methods for BeanPostProcessors must be static, so they live in the companion.
                (configClass.declaredClasses
                    .filter { it.simpleName == "Companion" }
                    .flatMap { it.declaredMethods.toList() }
                    .filter { it.isAnnotationPresent(Bean::class.java) })

        /** Mirrors Spring's rule: explicit `@Bean` names win, otherwise the method name is the bean name. */
        fun beanNamesOf(method: Method): List<String> {
            val declared = method.getAnnotation(Bean::class.java).value
            return if (declared.isEmpty()) listOf(method.name) else declared.toList()
        }

        /** Default value of an annotation attribute, e.g. `TenantCacheable.keyGenerator`. */
        fun defaultOf(annotation: Class<out Annotation>, attribute: String): String =
            annotation.getDeclaredMethod(attribute).defaultValue as String

        /** Every `@Aspect` class shipped by this module, found the same way Spring would scan for them. */
        fun aspectClassesInModule(): List<Class<*>> {
            val scanner = ClassPathScanningCandidateComponentProvider(false).apply {
                addIncludeFilter(AnnotationTypeFilter(Aspect::class.java))
            }
            return scanner.findCandidateComponents(BASE_PACKAGE)
                .mapNotNull { (it as? AnnotatedBeanDefinition)?.metadata?.className }
                // initialize = false: merely *loading* an aspect must not run its static initializers, some of
                // which reach into the Spring container (see DistributedCacheGuardAspect's lazily resolved
                // lockProvider) and would fail here with no context running.
                .map { Class.forName(it, false, CacheWiringCompletenessTest::class.java.classLoader) }
        }
    }

    // ---- 切面必须被显式注册 ---------------------------------------------------

    @Test
    fun everyAspectInModuleIsRegisteredAsBean() {
        val aspects = aspectClassesInModule()
        assertTrue(aspects.isNotEmpty(), "未扫描到任何 @Aspect，测试本身失效了（包名是否改过？）")

        val unregistered = aspects.filterNot { aspect -> declaredBeanTypes.any { aspect.isAssignableFrom(it) } }

        assertTrue(
            unregistered.isEmpty(),
            "以下切面带 @Aspect 但没有任何 @Bean 方法注册它们；kudos 不扫描 io.kudos.ability.**，" +
                "所以它们在运行期完全不生效（对应注解静默失效）：\n" +
                unregistered.joinToString("\n") { "  - ${it.name}" }
        )
    }

    // ---- 注解默认引用的 keyGenerator 必须真实存在 -----------------------------

    @Test
    fun keyGeneratorNamedByTenantAnnotationsIsDeclared() {
        // Spring Cache 按 bean 名解析 keyGenerator 属性；名字对不上就是运行期 NoSuchBeanDefinitionException。
        val referenced = listOf(
            TenantCacheable::class.java to defaultOf(TenantCacheable::class.java, "keyGenerator"),
            TenantCachePut::class.java to defaultOf(TenantCachePut::class.java, "keyGenerator"),
            TenantCacheEvict::class.java to defaultOf(TenantCacheEvict::class.java, "keyGenerator"),
        )

        referenced.forEach { (annotation, beanName) ->
            assertTrue(
                beanName in declaredBeanNames,
                "@${annotation.simpleName} 的 keyGenerator 默认值是 \"$beanName\"，但没有任何 @Bean 方法以该名字注册。" +
                    "已声明的 bean 名: ${declaredBeanNames.sorted()}"
            )
        }
    }

    @Test
    fun tenantAnnotationsShareTheSameKeyGenerator() {
        val names = setOf(
            defaultOf(TenantCacheable::class.java, "keyGenerator"),
            defaultOf(TenantCachePut::class.java, "keyGenerator"),
            defaultOf(TenantCacheEvict::class.java, "keyGenerator"),
        )
        assertEquals(
            1, names.size,
            "@Tenant* 三个注解应共用同一个 keyGenerator，否则租户 key 规则会不一致：$names"
        )
    }

    // ---- 关键协作 bean 必须存在 ----------------------------------------------

    @Test
    fun coreCollaboratorBeansAreDeclared() {
        // 这些名字被别处按名字注入（@Resource(name=) / @Qualifier / 注解属性），改名或漏声明都是运行期才炸。
        val required = listOf(
            "cacheManager",            // @Primary CacheManager，Spring Cache 的入口
            "mixCacheManager",         // KeyValueCacheKit / RedisCacheMessageHandler 按名取
            "mixHashCacheManager",     // HashCacheKit 按名取
            "tenantCacheKeyGenerator", // @Tenant* 注解默认引用
            "contextKeyGenerator",     // @CacheKey 使用
            "simpleKeyGenerator",
            "defaultKeysGenerator",    // @BatchCacheable 默认引用
            "defaultHashBatchKeysGenerator",
        )
        val missing = required.filterNot { it in declaredBeanNames }
        assertTrue(
            missing.isEmpty(),
            "以下 bean 名被按名字消费但未声明: $missing；已声明: ${declaredBeanNames.sorted()}"
        )
    }

    @Test
    fun batchCacheableDefaultKeysGeneratorIsDeclared() {
        val beanName = defaultOf(
            io.kudos.ability.cache.common.batch.keyvalue.BatchCacheable::class.java, "keysGenerator"
        )
        assertTrue(
            beanName in declaredBeanNames,
            "@BatchCacheable.keysGenerator 默认值 \"$beanName\" 没有对应的 @Bean 声明"
        )
    }

    // ---- 共享 bean 只能有一个属主 --------------------------------------------

    @Test
    fun sharedBeansAreNotDeclaredTwiceWithDifferentSemantics() {
        // BaseCacheConfiguration 与 LinkableCacheAutoConfiguration 都声明这批 bean（前者是给业务方手工装配用的
        // 可选 mixin）。两边必须等价，否则谁先注册谁生效——历史上 Base 版少了 @ConfigurationProperties，
        // 一旦它赢下这场竞争，cache-items 就完全不绑定，缓存一个都建不出来。
        val linkable = beanMethodsOf(LinkableCacheAutoConfiguration::class.java).associateBy { beanNamesOf(it).first() }
        val base = beanMethodsOf(BaseCacheConfiguration::class.java).associateBy { beanNamesOf(it).first() }

        val overlapping = linkable.keys intersect base.keys
        assertTrue(overlapping.isNotEmpty(), "两个配置类不再有重叠 bean，本测试可以删除了")

        overlapping.forEach { name ->
            val a = linkable.getValue(name)
            val b = base.getValue(name)
            val aProps = a.isAnnotationPresent(org.springframework.boot.context.properties.ConfigurationProperties::class.java)
            val bProps = b.isAnnotationPresent(org.springframework.boot.context.properties.ConfigurationProperties::class.java)
            if (aProps != bProps) {
                fail(
                    "bean \"$name\" 在两个配置类中对 @ConfigurationProperties 的声明不一致 " +
                        "(LinkableCacheAutoConfiguration=$aProps, BaseCacheConfiguration=$bProps)。" +
                        "两者靠 @ConditionalOnMissingBean 竞争，赢家不确定，因此必须等价。"
                )
            }
        }
    }
}
