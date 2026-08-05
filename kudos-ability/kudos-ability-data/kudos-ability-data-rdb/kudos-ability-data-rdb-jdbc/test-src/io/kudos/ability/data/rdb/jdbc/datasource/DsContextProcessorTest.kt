package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource
import com.baomidou.dynamic.datasource.creator.DataSourceProperty
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator
import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider
import com.zaxxer.hikari.HikariDataSource
import io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import javax.sql.DataSource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [DsContextProcessor] with reflection-injected collaborators (no Spring container).
 *
 * Covers: context-missing / console-tenant short circuits, master vs readonly resolution,
 * [IDataSourceFinder] override, on-the-fly data source creation + optional [IDataSourceProxy]
 * wrapping, the unconfigured-dsId failure mode, single- vs multi-data-source degradation of
 * getDataSource / haveDataSource / currentDataSource, and refreshDatasource for one / all entries.
 *
 * @author K
 * @since 1.0.0
 */
internal class DsContextProcessorTest {

    private lateinit var masterDs: HikariDataSource
    private lateinit var ds7: HikariDataSource
    private lateinit var routing: DynamicRoutingDataSource

    @BeforeTest
    fun setUp() {
        KudosContextHolder.clear()
        masterDs = HikariDataSource()
        ds7 = HikariDataSource()
        routing = DynamicRoutingDataSource(listOf(DynamicDataSourceProvider {
            mapOf<String, DataSource>("master" to masterDs, "ds7" to ds7)
        }))
        routing.setPrimary("master")
        routing.afterPropertiesSet()
    }

    @AfterTest
    fun tearDown() {
        KudosContextHolder.clear()
    }

    private fun newProcessor(
        dataSource: DataSource = routing,
        creator: DefaultDataSourceCreator = StubCreator(HikariDataSource()),
        load: IDynamicDataSourceLoad = StubLoad(emptyMap()),
        proxy: IDataSourceProxy? = null,
        finder: IDataSourceFinder? = null,
        primary: String = "master",
    ): DsContextProcessor {
        val p = DsContextProcessor()
        inject(p, "dataSource", dataSource)
        inject(p, "dataSourceCreator", creator)
        inject(p, "dynamicDataSourceLoad", load)
        inject(p, "dataSourceProxy", proxy)
        inject(p, "dataSourceFinder", finder)
        inject(p, "primary", primary)
        return p
    }

    // ----- doDetermineDatasource -----

    @Test
    fun doDetermine_returnsNullWithoutContext() {
        KudosContextHolder.clear()
        assertNull(newProcessor().doDetermineDatasource("svc::t1::master", "_context::svc"))
    }

    @Test
    fun doDetermine_consoleTenantSkipsRouting() {
        KudosContextHolder.set(KudosContext().apply {
            _datasourceTenantId = DatasourceConst.CONSOLE_TENANT_ID
            dataSourceId = "ds7"
        })
        assertNull(newProcessor().doDetermineDatasource("svc::t1::master", "_context::svc"))
    }

    @Test
    fun doDetermine_masterMode_usesContextDataSourceId() {
        KudosContextHolder.set(KudosContext().apply {
            _datasourceTenantId = "t1"
            dataSourceId = "ds7"
        })
        val key = newProcessor().doDetermineDatasource("_context::svc::t1::master", "_context::svc")
        assertEquals("ds7", key, "ds7 already lives in the routing table; no dynamic load needed")
    }

    @Test
    fun doDetermine_readonlySuffix_usesReadOnlyDataSourceId_andLoadsOnTheFly() {
        KudosContextHolder.set(KudosContext().apply {
            _datasourceTenantId = "t1"
            dataSourceId = "ds7"
            readOnlyDataSourceId = "dsRo"
        })
        val created = HikariDataSource()
        val creator = StubCreator(created)
        val load = StubLoad(mapOf("dsRo" to DataSourceProperty().apply { poolName = "dsRo" }))

        val key = newProcessor(creator = creator, load = load)
            .doDetermineDatasource("_context::svc::t1::readonly", "_context::svc")

        assertEquals("dsRo", key)
        assertEquals(1, creator.calls, "missing entry must be created exactly once")
        assertSame(created, routing.dataSources["dsRo"], "the freshly created source is registered as-is (no proxy)")
    }

    @Test
    fun doDetermine_readonlySuffixWithoutReadOnlyId_fallsBackToMaster() {
        KudosContextHolder.set(KudosContext().apply {
            _datasourceTenantId = "t1"
            dataSourceId = "ds7"
            readOnlyDataSourceId = null
        })
        val key = newProcessor().doDetermineDatasource("_context::svc::t1::readonly", "_context::svc")
        assertEquals("ds7", key)
    }

    @Test
    fun doDetermine_finderOverridesContextDefault() {
        KudosContextHolder.set(KudosContext().apply {
            _datasourceTenantId = "t1"
            dataSourceId = "ignored"
        })
        val finder = RecordingFinder(answer = "ds7")
        val key = newProcessor(finder = finder).doDetermineDatasource("_context::svcF::t1::master", "_context::svcF")

        assertEquals("ds7", key)
        assertEquals<List<Triple<String?, String?, String?>>>(
            listOf(Triple("t1", "svcF", DatasourceConst.MODE_MASTER)), finder.calls,
            "finder receives tenant id, server code parsed from the dsKey, and the mode"
        )
    }

    @Test
    fun doDetermine_finderReturnsNull_fallsBackToContextDefault() {
        KudosContextHolder.set(KudosContext().apply {
            _datasourceTenantId = "t1"
            dataSourceId = "ds7"
        })
        val key = newProcessor(finder = RecordingFinder(answer = null))
            .doDetermineDatasource("_context::svcG::t1::master", "_context::svcG")
        assertEquals("ds7", key)
    }

    @Test
    fun doDetermine_missingDsWithoutConfig_throws() {
        KudosContextHolder.set(KudosContext().apply {
            _datasourceTenantId = "t1"
            dataSourceId = "ghost"
        })
        val e = assertFailsWith<RuntimeException> {
            newProcessor(load = StubLoad(emptyMap()))
                .doDetermineDatasource("_context::svc::t1::master", "_context::svc")
        }
        assertTrue(e.message!!.contains("dsId=ghost"), "error must name the unconfigured dsId")
    }

    @Test
    fun doDetermine_proxyWrapsTheCreatedDataSource() {
        KudosContextHolder.set(KudosContext().apply {
            _datasourceTenantId = "t1"
            dataSourceId = "dsNew"
        })
        val created = HikariDataSource()
        val wrapped = HikariDataSource()
        val proxy = object : IDataSourceProxy {
            override fun proxyDatasource(dataSource: DataSource): DataSource = wrapped
        }
        val key = newProcessor(
            creator = StubCreator(created),
            load = StubLoad(mapOf("dsNew" to DataSourceProperty().apply { poolName = "dsNew" })),
            proxy = proxy,
        ).doDetermineDatasource("_context::svcP::t1::master", "_context::svcP")

        assertEquals("dsNew", key)
        assertSame(wrapped, routing.dataSources["dsNew"], "the proxy-wrapped instance must be registered")
    }

    // ----- degradation / lookups -----

    @Test
    fun getDataSource_haveDataSource_currentDataSource_withDynamicRouting() {
        val p = newProcessor()
        assertSame(ds7, p.getDataSource("ds7"))
        assertTrue(p.haveDataSource("ds7"))
        assertFalse(p.haveDataSource("nope"))
        assertSame(masterDs, p.currentDataSource(), "no thread-local key set -> primary")
    }

    @Test
    fun singleDataSourceScenario_degradesGracefully() {
        val plain = HikariDataSource()
        val p = newProcessor(dataSource = plain)
        assertSame(plain, p.getDataSource("whatever"), "non-routing data source ignores the key")
        assertTrue(p.haveDataSource("anything"), "single data source: always 'present'")
        assertSame(plain, p.currentDataSource())
    }

    // ----- refreshDatasource -----

    @Test
    fun refreshDatasource_singleId_removesThatEntry() {
        routing.addDataSource("8", HikariDataSource())
        assertTrue(routing.dataSources.containsKey("8"))

        newProcessor().refreshDatasource(8)

        assertFalse(routing.dataSources.containsKey("8"))
        assertTrue(routing.dataSources.containsKey("master"), "other entries untouched")
    }

    @Test
    fun refreshDatasource_null_reloadsEverythingExceptPrimary() {
        routing.addDataSource("stale", HikariDataSource())

        newProcessor().refreshDatasource(null)

        assertFalse(routing.dataSources.containsKey("stale"), "non-primary entries are cleared")
        assertTrue(routing.dataSources.containsKey("master"), "primary survives the refresh")
        assertNotNull(routing.dataSources["ds7"], "provider-backed entries are re-loaded by afterPropertiesSet")
    }

    // ----- helpers -----

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

    /** returns a fixed product and counts invocations */
    private class StubCreator(private val product: DataSource) : DefaultDataSourceCreator() {
        var calls = 0
        override fun createDataSource(dataSourceProperty: DataSourceProperty): DataSource {
            calls++
            return product
        }
    }

    /** serves DataSourceProperty instances from a fixed map */
    private class StubLoad(private val map: Map<String, DataSourceProperty>) : IDynamicDataSourceLoad {
        override fun getPropertyById(dsId: String?): DataSourceProperty? = map[dsId]
    }

    /** records finder lookups and answers with a fixed id */
    private class RecordingFinder(private val answer: String?) : IDataSourceFinder {
        val calls = mutableListOf<Triple<String?, String?, String?>>()
        override fun findDataSourceId(tenantId: String?, serverCode: String?, mode: String?): String? {
            calls += Triple(tenantId, serverCode, mode)
            return answer
        }
    }
}
