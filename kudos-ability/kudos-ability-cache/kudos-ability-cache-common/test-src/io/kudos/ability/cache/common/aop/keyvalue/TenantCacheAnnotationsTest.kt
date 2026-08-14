package io.kudos.ability.cache.common.aop.keyvalue

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.SpringCacheAnnotationParser
import org.springframework.cache.interceptor.CacheableOperation
import org.springframework.cache.interceptor.CacheEvictOperation
import org.springframework.cache.interceptor.CachePutOperation
import org.springframework.core.annotation.AnnotatedElementUtils
import java.lang.reflect.Method
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Guards the composed-annotation contract of [TenantCacheable] / [TenantCachePut] / [TenantCacheEvict].
 *
 * Regression background: each of these three used to have its meta-annotation commented out
 * (`//@Cacheable`, `//@CachePut`, `//@CacheEvict`) while still declaring
 * `@AliasFor(annotation = Cacheable::class, ...)` on its attributes. That combination is doubly broken:
 *
 *  1. Spring Cache only recognises operations meta-annotated with its own annotations, so the whole
 *     `@Tenant*` family was inert — methods carrying them were never cached, evicted, or put.
 *  2. `@AliasFor(annotation = X)` on an annotation that is *not* meta-annotated with `X` is invalid, so
 *     merely reading the annotation through Spring threw `AnnotationConfigurationException`.
 *
 * The attribute aliases were also crossed (`value` -> `cacheNames` and `cacheNames` -> `value`) instead of
 * mapping straight through the way Spring's own composed annotations (e.g. `@GetMapping`) do.
 *
 * These tests assert against [SpringCacheAnnotationParser] — the exact component Spring Cache uses to turn a
 * method into cache operations — so a regression here fails loudly rather than silently disabling caching.
 *
 * @author K
 * @since 1.0.0
 */
internal class TenantCacheAnnotationsTest {

    private val parser = SpringCacheAnnotationParser()

    @Suppress("unused", "UNUSED_PARAMETER")
    class Ops {
        @TenantCacheable(cacheNames = ["kv-cache"], suffix = "#id")
        fun readByCacheNames(id: String): String = id

        @TenantCacheable("kv-cache")
        fun readByValue(id: String): String = id

        @TenantCachePut(cacheNames = ["kv-cache"], suffix = "#id")
        fun write(id: String): String = id

        @TenantCacheEvict(cacheNames = ["kv-cache"], suffix = "#id", allEntries = true)
        fun evictAll(id: String): String = id
    }

    private fun method(name: String): Method = Ops::class.java.declaredMethods.first { it.name == name }

    // ---- meta-annotation present --------------------------------------------

    @Test
    fun tenantCacheableIsMetaAnnotatedWithCacheable() {
        val merged = AnnotatedElementUtils.findMergedAnnotation(method("readByCacheNames"), Cacheable::class.java)
        assertNotNull(merged, "@TenantCacheable 必须元标注 @Cacheable，否则 Spring Cache 完全不认它")
        assertEquals(listOf("kv-cache"), merged.cacheNames.toList())
        assertEquals(
            "tenantCacheKeyGenerator", merged.keyGenerator,
            "keyGenerator 默认值应透传到 @Cacheable，租户维度才会进 key"
        )
    }

    @Test
    fun tenantCachePutIsMetaAnnotatedWithCachePut() {
        val merged = AnnotatedElementUtils.findMergedAnnotation(method("write"), CachePut::class.java)
        assertNotNull(merged, "@TenantCachePut 必须元标注 @CachePut")
        assertEquals(listOf("kv-cache"), merged.cacheNames.toList())
        assertEquals("tenantCacheKeyGenerator", merged.keyGenerator)
    }

    @Test
    fun tenantCacheEvictIsMetaAnnotatedWithCacheEvict() {
        val merged = AnnotatedElementUtils.findMergedAnnotation(method("evictAll"), CacheEvict::class.java)
        assertNotNull(merged, "@TenantCacheEvict 必须元标注 @CacheEvict")
        assertEquals(listOf("kv-cache"), merged.cacheNames.toList())
        assertEquals("tenantCacheKeyGenerator", merged.keyGenerator)
        assertTrue(merged.allEntries, "allEntries 应透传到 @CacheEvict")
    }

    // ---- value / cacheNames 互为镜像 -----------------------------------------

    @Test
    fun valueAndCacheNamesAreMirrors() {
        val merged = AnnotatedElementUtils.findMergedAnnotation(method("readByValue"), Cacheable::class.java)
        assertNotNull(merged, "以 value 形式书写时同样应解析出 @Cacheable")
        assertEquals(
            listOf("kv-cache"), merged.cacheNames.toList(),
            "@TenantCacheable(\"x\") 的 vararg value 应镜像到 cacheNames"
        )
    }

    // ---- Spring Cache 实际解析结果 -------------------------------------------

    @Test
    fun springCacheParserRecognisesTenantAnnotations() {
        val cacheable = parser.parseCacheAnnotations(method("readByCacheNames"))
        assertNotNull(cacheable, "SpringCacheAnnotationParser 未识别 @TenantCacheable —— 缓存不会生效")
        assertTrue(
            cacheable.single() is CacheableOperation,
            "@TenantCacheable 应解析为 CacheableOperation，实际: ${cacheable.single()::class.simpleName}"
        )

        val put = parser.parseCacheAnnotations(method("write"))
        assertNotNull(put, "SpringCacheAnnotationParser 未识别 @TenantCachePut")
        assertTrue(put.single() is CachePutOperation)

        val evict = parser.parseCacheAnnotations(method("evictAll"))
        assertNotNull(evict, "SpringCacheAnnotationParser 未识别 @TenantCacheEvict")
        assertTrue(evict.single() is CacheEvictOperation)
    }

    @Test
    fun parsedOperationCarriesTenantKeyGenerator() {
        val op = parser.parseCacheAnnotations(method("readByCacheNames"))!!.single()
        assertEquals(
            "tenantCacheKeyGenerator", op.keyGenerator,
            "解析出的 CacheOperation 必须带上 tenantCacheKeyGenerator，否则 key 不含租户维度"
        )
        assertEquals(
            "", op.key,
            "不应同时设置 key 与 keyGenerator —— Spring 会因两者互斥而抛异常"
        )
    }
}
