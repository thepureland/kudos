package io.kudos.ability.data.rdb.jdbc.aop

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder
import io.kudos.ability.data.rdb.jdbc.context.DbContext
import io.kudos.ability.data.rdb.jdbc.context.DbParam
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.ability.data.rdb.jdbc.datasource.RoutingCache
import io.kudos.ability.data.rdb.jdbc.init.MultipleDataSourceProperties
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import org.aopalliance.intercept.MethodInvocation
import java.lang.reflect.Proxy
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Unit tests for [DynamicDataSourceInterceptor]'s routing decisions, with the collaborator beans
 * injected by reflection and a recording [DsContextProcessor] stub (no Spring container).
 *
 * Covers: the forcedDs fast path (hit / readonly skip / `_context` skip / unknown-key skip),
 * package-path routing with a plain key, `_context` dynamic resolution for master / readonly /
 * tenant-forced variants, resolution-cache hits and invalidation, the null-resolution skip
 * semantics, the tenant dimension of the cache key, the no-ThreadLocal-pollution guarantee, and
 * stack pop on both normal and exceptional exits.
 *
 * @author K
 * @since 1.0.0
 */
internal class DynamicDataSourceInterceptorTest {

    /** the routing target whose package is matched against the configuration */
    private class TargetService {
        @Suppress("unused")
        fun noop() = Unit
    }

    private lateinit var props: MultipleDataSourceProperties
    private lateinit var processor: RecordingProcessor
    private lateinit var routingCache: RoutingCache
    private lateinit var interceptor: DynamicDataSourceInterceptor

    @BeforeTest
    fun setUp() {
        DbContext.clear()
        KudosContextHolder.clear()
        DynamicDataSourceContextHolder.clear()
        props = MultipleDataSourceProperties()
        processor = RecordingProcessor()
        routingCache = RoutingCache()
        interceptor = DynamicDataSourceInterceptor()
        inject(interceptor, "dataSourceProperties", props)
        inject(interceptor, "dsContextProcessor", processor)
        inject(interceptor, "routingCache", routingCache)
        KudosContextHolder.set(KudosContext().apply { dataSourceId = "tenant1" })
    }

    @AfterTest
    fun tearDown() {
        DbContext.clear()
        KudosContextHolder.clear()
        DynamicDataSourceContextHolder.clear()
    }

    @Test
    fun noConfigAndNoForce_doesNotTouchTheStack() {
        var peeked: String? = "sentinel"
        val result = interceptor.invoke(invocation { peeked = DynamicDataSourceContextHolder.peek(); "ok" })
        assertEquals("ok", result)
        assertNull(peeked, "no routing config -> inherit whatever the upper stack set (nothing here)")
        assertNull(DynamicDataSourceContextHolder.peek())
    }

    @Test
    fun interceptor_neverSeedsDbContext() {
        DbContext.clear()
        interceptor.invoke(invocation { })
        assertNull(
            DbContext.getOrNull(),
            "routing must not create ThreadLocal state as a side effect (thread-pool leak source)"
        )
    }

    @Test
    fun forcedDs_fastPath_pushesAndPops() {
        DbContext.set(DbParam(forcedDs = "dsX", enableLog = true))
        processor.existingKeys += "dsX"

        var inside: String? = null
        interceptor.invoke(invocation { inside = DynamicDataSourceContextHolder.peek() })

        assertEquals("dsX", inside, "forced key must be visible to the business method")
        assertNull(DynamicDataSourceContextHolder.peek(), "popped after the call")
        assertEquals(0, processor.determineCalls.size, "fast path never does dynamic resolution")
    }

    @Test
    fun forcedDs_unknownKey_fallsThroughToPackageRouting() {
        DbContext.set(DbParam(forcedDs = "ghost")) // not in existingKeys
        props.packageDataSource[targetPackage()] = "dsPlain"

        var inside: String? = null
        interceptor.invoke(invocation { inside = DynamicDataSourceContextHolder.peek() })

        assertEquals("dsPlain", inside, "unreachable forced key is skipped; package config wins")
        assertNull(DynamicDataSourceContextHolder.peek())
    }

    @Test
    fun plainPackageConfig_pushesConfiguredKey() {
        props.packageDataSource[targetPackage()] = "dsZ"

        var inside: String? = null
        interceptor.invoke(invocation { inside = DynamicDataSourceContextHolder.peek() })

        assertEquals("dsZ", inside)
        assertNull(DynamicDataSourceContextHolder.peek())
    }

    @Test
    fun contextConfig_masterByDefault_andResultIsCached() {
        props.packageDataSource[targetPackage()] = "_context::svcA"
        processor.resolveTo = "resolvedA"

        var inside: String? = null
        interceptor.invoke(invocation { inside = DynamicDataSourceContextHolder.peek() })
        assertEquals("resolvedA", inside)
        assertEquals<List<Pair<String, String?>>>(
            listOf("_context::svcA::tenant1::master" to "_context::svcA"),
            processor.determineCalls,
            "mapKey is <config>::<tenantDimension>::master"
        )

        // second call hits the resolution cache — no new processor invocation
        interceptor.invoke(invocation { })
        assertEquals(1, processor.determineCalls.size)

        // cache invalidation forces re-resolution
        routingCache.invalidateAll()
        interceptor.invoke(invocation { })
        assertEquals(2, processor.determineCalls.size)
    }

    @Test
    fun cacheKey_prefersDatasourceTenantIdOverDataSourceId() {
        props.packageDataSource[targetPackage()] = "_context::svcT"
        KudosContextHolder.set(KudosContext().apply { _datasourceTenantId = "tenantA"; dataSourceId = "sharedDs" })
        processor.resolveTo = "resolvedA"
        interceptor.invoke(invocation { })
        assertEquals<List<Pair<String, String?>>>(
            listOf("_context::svcT::tenantA::master" to "_context::svcT"),
            processor.determineCalls
        )

        // a second tenant sharing the same context dataSourceId must NOT hit tenantA's cache entry
        KudosContextHolder.set(KudosContext().apply { _datasourceTenantId = "tenantB"; dataSourceId = "sharedDs" })
        processor.resolveTo = "resolvedB"
        var inside: String? = null
        interceptor.invoke(invocation { inside = DynamicDataSourceContextHolder.peek() })
        assertEquals("resolvedB", inside)
        assertEquals(2, processor.determineCalls.size, "different tenants resolve independently")
    }

    @Test
    fun contextConfig_readonlyForced_usesReadonlySuffix() {
        DbContext.set(DbParam(forcedDs = "anything", readonly = true))
        props.packageDataSource[targetPackage()] = "_context::svcB"
        processor.resolveTo = "resolvedRo"

        var inside: String? = null
        interceptor.invoke(invocation { inside = DynamicDataSourceContextHolder.peek() })

        assertEquals("resolvedRo", inside)
        assertEquals<List<Pair<String, String?>>>(
            listOf("_context::svcB::tenant1::readonly" to "_context::svcB"),
            processor.determineCalls,
            "readonly intent must select the readonly mode suffix while keeping the original config as key"
        )
    }

    @Test
    fun contextConfig_readonlyWithoutForcedDs_stillUsesReadonlySuffix() {
        // e.g. @DsChange(readonly = true) with a blank value: the intent alone selects the replica
        DbContext.set(DbParam(readonly = true))
        props.packageDataSource[targetPackage()] = "_context::svcRo"
        processor.resolveTo = "resolvedRoOnly"

        var inside: String? = null
        interceptor.invoke(invocation { inside = DynamicDataSourceContextHolder.peek() })

        assertEquals("resolvedRoOnly", inside)
        assertEquals<List<Pair<String, String?>>>(
            listOf("_context::svcRo::tenant1::readonly" to "_context::svcRo"),
            processor.determineCalls
        )
    }

    @Test
    fun contextConfig_tenantForcedNotReadonly_usesForcedDsAsKey() {
        // _context-prefixed forcedDs skips the fast path and becomes the cache key (TenantDsChange adaptation)
        DbContext.set(DbParam(forcedDs = "_context::svcOther"))
        props.packageDataSource[targetPackage()] = "_context::svcC"
        processor.resolveTo = "resolvedT"

        var inside: String? = null
        interceptor.invoke(invocation { inside = DynamicDataSourceContextHolder.peek() })

        assertEquals("resolvedT", inside)
        assertEquals<List<Pair<String, String?>>>(
            listOf("_context::svcOther::tenant1::master" to "_context::svcC"),
            processor.determineCalls
        )
    }

    @Test
    fun contextConfig_nullResolution_skipsSwitchingAndDoesNotCache() {
        // null from doDetermineDatasource means "no context / console tenant" — proceed unswitched
        props.packageDataSource[targetPackage()] = "_context::svcNull"
        processor.resolveTo = null

        var inside: String? = "sentinel"
        val result = interceptor.invoke(invocation { inside = DynamicDataSourceContextHolder.peek(); "ok" })

        assertEquals("ok", result)
        assertNull(inside, "nothing pushed when resolution declines")
        assertNull(DynamicDataSourceContextHolder.peek())
        assertEquals(1, processor.determineCalls.size)

        // a null resolution must not be cached: the context may appear on the next call
        interceptor.invoke(invocation { })
        assertEquals(2, processor.determineCalls.size)
    }

    @Test
    fun exceptionFromBusinessMethod_stillPopsTheStack() {
        props.packageDataSource[targetPackage()] = "dsBoom"

        assertFailsWith<IllegalStateException> {
            interceptor.invoke(invocation { error("boom") })
        }
        assertNull(DynamicDataSourceContextHolder.peek(), "finally must pop even on exception")
    }

    // ----- helpers -----

    private fun targetPackage(): String = TargetService::class.java.packageName

    private fun invocation(proceed: () -> Any?): MethodInvocation {
        val target = TargetService()
        return Proxy.newProxyInstance(
            MethodInvocation::class.java.classLoader,
            arrayOf(MethodInvocation::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "proceed" -> proceed()
                "getThis" -> target
                "getMethod" -> TargetService::class.java.getDeclaredMethod("noop")
                "getArguments" -> emptyArray<Any>()
                "toString" -> "testInvocation"
                else -> null
            }
        } as MethodInvocation
    }

    private fun inject(target: Any, fieldName: String, value: Any?) {
        var cls: Class<*>? = target.javaClass
        while (cls != null) {
            try {
                val f = cls.getDeclaredField(fieldName)
                f.isAccessible = true
                f.set(target, value)
                return
            } catch (_: NoSuchFieldException) {
                cls = cls.superclass
            }
        }
        throw NoSuchFieldException(fieldName)
    }

    /** records resolution requests and answers with a configurable key; routing table membership is scriptable */
    private class RecordingProcessor : DsContextProcessor() {
        val existingKeys = mutableSetOf<String>()
        val determineCalls = mutableListOf<Pair<String, String?>>()
        var resolveTo: String? = null

        override fun haveDataSource(dsKey: String?): Boolean = dsKey in existingKeys

        override fun doDetermineDatasource(dsKey: String, dsKeyConfig: String?): String? {
            determineCalls += dsKey to dsKeyConfig
            return resolveTo
        }
    }
}
