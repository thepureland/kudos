package io.kudos.ability.data.rdb.jdbc.aop

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder
import io.kudos.ability.data.rdb.jdbc.context.DbContext
import io.kudos.ability.data.rdb.jdbc.context.DbParam
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.ability.data.rdb.jdbc.init.MultipleDataSourceProperties
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import org.aspectj.lang.ProceedingJoinPoint
import java.lang.reflect.Proxy
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [DynamicDataSourceAspect]'s routing decisions, with the collaborator beans
 * injected by reflection and a recording [DsContextProcessor] stub (no Spring container).
 *
 * Covers: the forcedDs fast path (hit / readonly skip / `_context` skip / unknown-key skip),
 * package-path routing with a plain key, `_context` dynamic resolution for master / readonly /
 * tenant-forced variants, resolution-cache hits and invalidation via cacheDsCache(), the
 * null-resolution failure mode, and stack pop on both normal and exceptional exits.
 *
 * @author K
 * @since 1.0.0
 */
internal class DynamicDataSourceAspectTest {

    /** the routing target whose package is matched against the configuration */
    private class TargetService

    private lateinit var props: MultipleDataSourceProperties
    private lateinit var processor: RecordingProcessor
    private lateinit var aspect: DynamicDataSourceAspect

    @BeforeTest
    fun setUp() {
        DynamicDataSourceAspect.cacheDsCache()
        DbContext.clear()
        KudosContextHolder.clear()
        DynamicDataSourceContextHolder.clear()
        props = MultipleDataSourceProperties()
        processor = RecordingProcessor()
        aspect = DynamicDataSourceAspect()
        inject(aspect, "dataSourceProperties", props)
        inject(aspect, "dsContextProcessor", processor)
        KudosContextHolder.set(KudosContext().apply { dataSourceId = "tenant1" })
    }

    @AfterTest
    fun tearDown() {
        DynamicDataSourceAspect.cacheDsCache()
        DbContext.clear()
        KudosContextHolder.clear()
        DynamicDataSourceContextHolder.clear()
    }

    @Test
    fun noConfigAndNoForce_doesNotTouchTheStack() {
        var peeked: String? = "sentinel"
        val result = aspect.around(joinPoint { peeked = DynamicDataSourceContextHolder.peek(); "ok" })
        assertEquals("ok", result)
        assertNull(peeked, "no routing config -> inherit whatever the upper stack set (nothing here)")
        assertNull(DynamicDataSourceContextHolder.peek())
    }

    @Test
    fun forcedDs_fastPath_pushesAndPops() {
        DbContext.set(DbParam().apply { forcedDs = "dsX"; enableLog = true })
        processor.existingKeys += "dsX"

        var inside: String? = null
        aspect.around(joinPoint { inside = DynamicDataSourceContextHolder.peek() })

        assertEquals("dsX", inside, "forced key must be visible to the business method")
        assertNull(DynamicDataSourceContextHolder.peek(), "popped after the call")
        assertEquals(0, processor.determineCalls.size, "fast path never does dynamic resolution")
    }

    @Test
    fun forcedDs_unknownKey_fallsThroughToPackageRouting() {
        DbContext.set(DbParam().apply { forcedDs = "ghost" }) // not in existingKeys
        props.packageDataSource[targetPackage()] = "dsPlain"

        var inside: String? = null
        aspect.around(joinPoint { inside = DynamicDataSourceContextHolder.peek() })

        assertEquals("dsPlain", inside, "unreachable forced key is skipped; package config wins")
        assertNull(DynamicDataSourceContextHolder.peek())
    }

    @Test
    fun plainPackageConfig_pushesConfiguredKey() {
        props.packageDataSource[targetPackage()] = "dsZ"

        var inside: String? = null
        aspect.around(joinPoint { inside = DynamicDataSourceContextHolder.peek() })

        assertEquals("dsZ", inside)
        assertNull(DynamicDataSourceContextHolder.peek())
    }

    @Test
    fun contextConfig_masterByDefault_andResultIsCached() {
        props.packageDataSource[targetPackage()] = "_context::svcA"
        processor.resolveTo = "resolvedA"

        var inside: String? = null
        aspect.around(joinPoint { inside = DynamicDataSourceContextHolder.peek() })
        assertEquals("resolvedA", inside)
        assertEquals<List<Pair<String, String?>>>(
            listOf("_context::svcA::tenant1::master" to "_context::svcA"),
            processor.determineCalls,
            "mapKey is <config>::<contextDataSourceId>::master"
        )

        // second call hits the static resolution cache — no new processor invocation
        aspect.around(joinPoint { })
        assertEquals(1, processor.determineCalls.size)

        // cache invalidation forces re-resolution
        DynamicDataSourceAspect.cacheDsCache()
        aspect.around(joinPoint { })
        assertEquals(2, processor.determineCalls.size)
    }

    @Test
    fun contextConfig_readonlyForced_usesReadonlySuffix() {
        DbContext.set(DbParam().apply { forcedDs = "anything"; readonly = true })
        props.packageDataSource[targetPackage()] = "_context::svcB"
        processor.resolveTo = "resolvedRo"

        var inside: String? = null
        aspect.around(joinPoint { inside = DynamicDataSourceContextHolder.peek() })

        assertEquals("resolvedRo", inside)
        assertEquals<List<Pair<String, String?>>>(
            listOf("_context::svcB::tenant1::readonly" to "_context::svcB"),
            processor.determineCalls,
            "readonly intent must select the readonly mode suffix while keeping the original config as key"
        )
    }

    @Test
    fun contextConfig_tenantForcedNotReadonly_usesForcedDsAsKey() {
        // _context-prefixed forcedDs skips the fast path and becomes the cache key (TenantDsChange adaptation)
        DbContext.set(DbParam().apply { forcedDs = "_context::svcOther" })
        props.packageDataSource[targetPackage()] = "_context::svcC"
        processor.resolveTo = "resolvedT"

        var inside: String? = null
        aspect.around(joinPoint { inside = DynamicDataSourceContextHolder.peek() })

        assertEquals("resolvedT", inside)
        assertEquals<List<Pair<String, String?>>>(
            listOf("_context::svcOther::tenant1::master" to "_context::svcC"),
            processor.determineCalls
        )
    }

    @Test
    fun contextConfig_nullResolution_throwsWithKeyInMessage() {
        props.packageDataSource[targetPackage()] = "_context::svcNull"
        processor.resolveTo = null

        val e = assertFailsWith<IllegalArgumentException> { aspect.around(joinPoint { "never" }) }
        assertTrue(e.message!!.contains("doDetermineDatasource returned null"))
        assertNull(DynamicDataSourceContextHolder.peek(), "nothing pushed when resolution fails")
    }

    @Test
    fun exceptionFromBusinessMethod_stillPopsTheStack() {
        props.packageDataSource[targetPackage()] = "dsBoom"

        assertFailsWith<IllegalStateException> {
            aspect.around(joinPoint { error("boom") })
        }
        assertNull(DynamicDataSourceContextHolder.peek(), "finally must pop even on exception")
    }

    @Test
    fun pointcutPlaceholder_isInvokable() {
        aspect.aspService() // pointcut method body is empty; invoke for completeness
    }

    // ----- helpers -----

    private fun targetPackage(): String = TargetService::class.java.packageName

    private fun joinPoint(proceed: () -> Any?): ProceedingJoinPoint {
        val target = TargetService()
        return Proxy.newProxyInstance(
            ProceedingJoinPoint::class.java.classLoader,
            arrayOf(ProceedingJoinPoint::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "proceed" -> proceed()
                "getTarget", "getThis" -> target
                "toString", "toShortString", "toLongString" -> "testJoinPoint"
                else -> null
            }
        } as ProceedingJoinPoint
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
