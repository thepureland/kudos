package io.kudos.ability.cache.common.aop.keyvalue

import io.kudos.ability.cache.common.aop.FakeProceedingJoinPoint
import io.kudos.ability.cache.common.aop.methodOf
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.support.TenantCacheKeyGenerator
import io.kudos.context.core.KudosContextHolder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.springframework.cache.Cache
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.Callable
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * [TenantCachingAspect] 单元测试。
 *
 * 注意：@TenantCaching 内嵌的 @TenantCacheEvict 声明了指向未元标注 @CacheEvict 的 @AliasFor，
 * 任何携带该注解的方法所在的 Spring bean 在容器刷新阶段都会抛 AnnotationConfigurationException
 * （详见 suspectedBugs 记录）。因此本测试不经 Spring AOP 代理，而是：
 *  - 手工实例化切面并通过反射注入 cacheManager（Mockito mock，evictByPattern 为 final 方法需 inline mock）、
 *    TenantCacheKeyGenerator 与 throwOnAfterCommitFailure；
 *  - 用假 ProceedingJoinPoint 直接调用 around。
 *
 * 覆盖：
 *  - beforeInvocation=false（默认）：无事务时方法执行后立即清缓存，顺序为 方法 → evict
 *  - beforeInvocation=true：方法执行前清缓存
 *  - allEntries=true：走 evictByPattern 清整个租户命名空间
 *  - getCache 返回 null：静默跳过
 *  - 多个 evict 配置与多个 cacheNames 按声明顺序逐一处理
 *  - suffix 为空时 key 仅为租户 id；非空时为 "tenantId::suffix结果"
 *  - 事务同步激活时：注册到 afterCommit 后才清缓存
 *  - afterCommit 清缓存失败：默认吞掉只记日志；throwOnAfterCommitFailure=true 时重抛原始异常
 *
 * @author K
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TenantCachingAspectTest {

    private val cacheManager: MixCacheManager = Mockito.mock(MixCacheManager::class.java)
    private val events = EventLog()
    private val c1 = RecordingCache("c1", events)
    private val c2 = RecordingCache("c2", events)
    private val boomCache = RecordingCache("boom", events).apply { throwOnEvict = true }
    private val aspect = buildAspect(throwOnFailure = false)
    private val holder = CachingOps()

    init {
        Mockito.`when`(cacheManager.getCache("c1")).thenReturn(c1)
        Mockito.`when`(cacheManager.getCache("c2")).thenReturn(c2)
        Mockito.`when`(cacheManager.getCache("boom")).thenReturn(boomCache)
        Mockito.`when`(cacheManager.getCache("missing")).thenReturn(null)
    }

    @AfterAll
    fun classTeardown() {
        KudosContextHolder.clear()
    }

    @BeforeTest
    fun reset() {
        events.clear()
        Mockito.clearInvocations(cacheManager)
        KudosContextHolder.get().tenantId = "T1"
    }

    @AfterTest
    fun cleanTxState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun pointcut_isInvocable() {
        aspect.cut()
    }

    @Test
    fun afterInvocation_noTransaction_evictsImmediatelyAfterMethod() {
        assertEquals("r:k1", invoke(aspect, "update", "k1"))
        assertEquals(listOf("method", "evict:c1:T1::k1"), events.snapshot(),
            "无事务时应在方法执行后立即按 租户::suffix key 清缓存")
    }

    @Test
    fun beforeInvocation_evictsBeforeMethod() {
        invoke(aspect, "updateBefore", "k2")

        assertEquals(listOf("evict:c1:T1::k2", "method"), events.snapshot(),
            "beforeInvocation=true 应先清缓存再执行方法")
    }

    @Test
    fun allEntries_evictsByPattern_notSingleKey() {
        invoke(aspect, "updateAll", "k3")

        Mockito.verify(cacheManager).evictByPattern("c1", "T1::k3")
        assertEquals(listOf("method"), events.snapshot(), "allEntries=true 不应做单 key evict")
    }

    @Test
    fun emptySuffix_keyIsTenantIdOnly() {
        invoke(aspect, "updateNoSuffix", "ignored")

        assertEquals(listOf("method", "evict:c1:T1"), events.snapshot(),
            "suffix 为空时 key 应只有租户 id")
    }

    @Test
    fun missingCache_isSilentlySkipped() {
        assertEquals("r:k4", invoke(aspect, "updateMissingCache", "k4"))
        assertEquals(listOf("method"), events.snapshot(), "getCache 为 null 时应跳过且不抛异常")
    }

    @Test
    fun multipleEvicts_andMultipleCacheNames_allProcessedInOrder() {
        invoke(aspect, "updateMulti", "k5")

        assertEquals(
            listOf("evict:c1:T1::k5", "method", "evict:c1:T1::k5", "evict:c2:T1::k5"),
            events.snapshot(),
            "前置 evict + 后置多 cacheName evict 应按声明顺序逐一执行"
        )
    }

    @Test
    fun activeTransaction_evictDeferredUntilAfterCommit() {
        TransactionSynchronizationManager.initSynchronization()
        try {
            invoke(aspect, "update", "k6")
            assertEquals(listOf("method"), events.snapshot(), "事务提交前不应清缓存")

            TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
            assertEquals(listOf("method", "evict:c1:T1::k6"), events.snapshot(), "afterCommit 后才应清缓存")
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun afterCommitEvictFailure_swallowedByDefault() {
        // boom 缓存 evict 抛异常；默认 throwOnAfterCommitFailure=false → 只记日志，不影响返回
        assertEquals("r:k7", invoke(aspect, "updateBoom", "k7"))
        assertEquals(listOf("method"), events.snapshot())
    }

    @Test
    fun afterCommitEvictFailure_rethrownWhenConfigured() {
        val throwingAspect = buildAspect(throwOnFailure = true)

        val e = assertFailsWith<IllegalStateException> { invoke(throwingAspect, "updateBoom", "k8") }
        assertEquals("evict boom", e.message, "配置重抛时应原样抛出原始异常")
    }

    // ---- 辅助 ---------------------------------------------------------------

    /** 用假 pjp 直接调 around；proceed 即记录 "method" 事件并返回 "r:<id>"。 */
    private fun invoke(aspect: TenantCachingAspect, methodName: String, id: String): Any? {
        val method = methodOf(CachingOps::class.java, methodName)
        val pjp = FakeProceedingJoinPoint(method, arrayOf(id), holder) {
            events.add("method")
            "r:$id"
        }
        return aspect.around(pjp)
    }

    /** 手工构造切面并通过反射注入私有协作字段。 */
    private fun buildAspect(throwOnFailure: Boolean): TenantCachingAspect {
        val aspect = TenantCachingAspect()
        setField(aspect, "cacheManager", cacheManager)
        setField(aspect, "tenantCacheKeyGenerator", TenantCacheKeyGenerator())
        setField(aspect, "throwOnAfterCommitFailure", throwOnFailure)
        return aspect
    }

    private fun setField(target: Any, name: String, value: Any?) {
        val field = target.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(target, value)
    }

    /** 外部事件记录器。 */
    class EventLog {
        private val entries = mutableListOf<String>()
        fun add(event: String) { entries.add(event) }
        fun snapshot(): List<String> = entries.toList()
        fun clear() { entries.clear() }
    }

    /** 记录 evict 的假 Cache；可配置 evict 抛异常。 */
    class RecordingCache(private val name: String, private val events: EventLog) : Cache {
        var throwOnEvict = false
        override fun getName(): String = name
        override fun getNativeCache(): Any = this
        override fun get(key: Any): Cache.ValueWrapper? = null
        override fun <T : Any> get(key: Any, type: Class<T>?): T? = null
        override fun <T : Any> get(key: Any, valueLoader: Callable<T>): T? = null
        override fun put(key: Any, value: Any?) {}
        override fun evict(key: Any) {
            if (throwOnEvict) throw IllegalStateException("evict boom")
            events.add("evict:$name:$key")
        }
        override fun clear() {}
    }

    /** 注解持有类（非 Spring bean，方法体不会被执行，proceed 由假 pjp 接管）。 */
    @Suppress("unused", "UNUSED_PARAMETER")
    internal class CachingOps {

        @TenantCaching(evicts = [TenantCacheEvict(cacheNames = ["c1"], suffix = "#id")])
        fun update(id: String): String = "x"

        @TenantCaching(evicts = [TenantCacheEvict(cacheNames = ["c1"], suffix = "#id", beforeInvocation = true)])
        fun updateBefore(id: String): String = "x"

        @TenantCaching(evicts = [TenantCacheEvict(cacheNames = ["c1"], suffix = "#id", allEntries = true)])
        fun updateAll(id: String): String = "x"

        @TenantCaching(evicts = [TenantCacheEvict(cacheNames = ["c1"])])
        fun updateNoSuffix(id: String): String = "x"

        @TenantCaching(evicts = [TenantCacheEvict(cacheNames = ["missing"], suffix = "#id")])
        fun updateMissingCache(id: String): String = "x"

        @TenantCaching(
            evicts = [
                TenantCacheEvict(cacheNames = ["c1"], suffix = "#id", beforeInvocation = true),
                TenantCacheEvict(cacheNames = ["c1", "c2"], suffix = "#id")
            ]
        )
        fun updateMulti(id: String): String = "x"

        @TenantCaching(evicts = [TenantCacheEvict(cacheNames = ["boom"], suffix = "#id")])
        fun updateBoom(id: String): String = "x"
    }
}
