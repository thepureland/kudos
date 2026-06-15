package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.creator.DataSourceCreator
import com.baomidou.dynamic.datasource.creator.DataSourceProperty
import com.baomidou.dynamic.datasource.ds.ItemDataSource
import com.baomidou.dynamic.datasource.enums.SeataMode
import com.baomidou.dynamic.datasource.event.DataSourceInitEvent
import com.baomidou.dynamic.datasource.toolkit.CryptoUtils
import com.zaxxer.hikari.HikariDataSource
import io.kudos.ability.data.rdb.jdbc.kit.DataSourceKit
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [DsDataSourceCreator] (no Spring container).
 *
 * Covers: missing-creators / unsupported-property failure modes, publicKey and lazy default
 * filling, the [DataSourceInitEvent] before/after hooks, schema + data script execution,
 * [ItemDataSource] wrapping with and without an [IDataSourceProxy], the Seata
 * forced-autoCommit fix, and the p6spy flag combination.
 *
 * @author K
 * @since 1.0.0
 */
internal class DsDataSourceCreatorTest {

    // NOTE: DataSourceProperty.publicKey defaults to null in baomidou 4.5.0, and
    // DsDataSourceCreator calls publicKey.isEmpty() without a null guard (NPE — recorded as a
    // suspected main-code bug). Tests set "" explicitly to exercise the intended fill-in branch.
    private fun newProperty(pool: String = "p1") = DataSourceProperty().apply {
        poolName = pool
        publicKey = ""
    }

    private fun supportingCreator(product: DataSource) = object : DataSourceCreator {
        override fun createDataSource(dataSourceProperty: DataSourceProperty): DataSource = product
        override fun support(dataSourceProperty: DataSourceProperty): Boolean = true
    }

    private fun rejectingCreator() = object : DataSourceCreator {
        override fun createDataSource(dataSourceProperty: DataSourceProperty): DataSource = error("unreachable")
        override fun support(dataSourceProperty: DataSourceProperty): Boolean = false
    }

    @Test
    fun createDataSource_withoutCreatorsThrows() {
        val e = assertFailsWith<IllegalStateException> { DsDataSourceCreator().createDataSource(newProperty()) }
        assertTrue(e.message!!.contains("creators must be set"))
    }

    @Test
    fun createDataSource_noSupportingCreatorThrows() {
        val creator = DsDataSourceCreator().apply { setCreators(listOf(rejectingCreator())) }
        val e = assertFailsWith<IllegalStateException> { creator.createDataSource(newProperty()) }
        assertTrue(e.message!!.contains("creator must not be null"))
    }

    @Test
    fun createDataSource_wrapsResultInItemDataSource() {
        val product = HikariDataSource()
        val creator = DsDataSourceCreator().apply { setCreators(listOf(supportingCreator(product))) }

        val result = creator.createDataSource(newProperty("poolA"))

        assertIs<ItemDataSource>(result)
        assertEquals("poolA", result.name)
        assertSame(product, result.realDataSource)
        assertSame(product, result.dataSource, "no proxy registered -> target stays the original")
        assertEquals(false, result.seata)
        assertNull(result.seataMode)
        assertEquals(false, result.p6spy)
    }

    @Test
    fun createDataSource_fillsGlobalPublicKeyAndLazyDefaults() {
        val creator = DsDataSourceCreator().apply { setCreators(listOf(supportingCreator(HikariDataSource()))) }
        val property = newProperty() // publicKey explicitly "" (see note above)
        assertNull(property.lazy)

        creator.createDataSource(property)

        assertEquals(CryptoUtils.DEFAULT_PUBLIC_KEY_STRING, property.publicKey)
        assertEquals(false, property.lazy, "global lazy default is false")
    }

    @Test
    fun createDataSource_respectsPresetPublicKeyAndLazy() {
        val creator = DsDataSourceCreator().apply {
            setCreators(listOf(supportingCreator(HikariDataSource())))
            setPublicKey("globalKey")
            setLazy(true)
        }
        val property = newProperty().apply {
            publicKey = "presetKey"
            lazy = false
        }

        creator.createDataSource(property)

        assertEquals("presetKey", property.publicKey, "non-empty property key must not be overwritten")
        assertEquals(false, property.lazy, "non-null property lazy must not be overwritten")
    }

    @Test
    fun createDataSource_appliesGlobalLazyWhenPropertyLazyIsNull() {
        val creator = DsDataSourceCreator().apply {
            setCreators(listOf(supportingCreator(HikariDataSource())))
            setLazy(true)
        }
        val property = newProperty()
        creator.createDataSource(property)
        assertEquals(true, property.lazy)
    }

    @Test
    fun createDataSource_firesInitEventHooksAroundCreation() {
        val events = mutableListOf<String>()
        val initEvent = object : DataSourceInitEvent {
            override fun beforeCreate(dataSourceProperty: DataSourceProperty) {
                events += "before:${dataSourceProperty.poolName}"
            }
            override fun afterCreate(dataSource: DataSource) {
                events += "after"
            }
        }
        val creator = DsDataSourceCreator().apply {
            setCreators(listOf(supportingCreator(HikariDataSource())))
            setDataSourceInitEvent(initEvent)
        }

        creator.createDataSource(newProperty("evt"))

        assertEquals(listOf("before:evt", "after"), events)
    }

    @Test
    fun createDataSource_runsSchemaAndDataScripts() {
        val real = DataSourceKit.createDataSource("jdbc:h2:mem:ds_creator_scripts;DB_CLOSE_DELAY=-1", "sa", "")
        try {
            val creator = DsDataSourceCreator().apply { setCreators(listOf(supportingCreator(real))) }
            val property = newProperty("scripted").apply {
                init.schema = "sql/creator-schema.sql"
                init.data = "sql/creator-data.sql"
            }

            creator.createDataSource(property)

            real.connection.use { conn ->
                conn.createStatement().use { st ->
                    val rs = st.executeQuery("select name from creator_init_test where id = 1")
                    assertTrue(rs.next(), "schema + data scripts must have run")
                    assertEquals("kudos", rs.getString(1))
                }
            }
        } finally {
            (real as HikariDataSource).close()
        }
    }

    @Test
    fun createDataSource_seataProxyForcesAutoCommitAndWraps() {
        val product = HikariDataSource()
        val proxied = HikariDataSource()
        val seataProxy = object : IDataSourceProxy {
            override fun proxyDatasource(dataSource: DataSource): DataSource = proxied
            override fun isSeata(): Boolean = true
            override fun seataMode(): SeataMode = SeataMode.AT
        }
        val creator = DsDataSourceCreator().apply { setCreators(listOf(supportingCreator(product))) }
        inject(creator, "dataSourceProxy", seataProxy)

        val property = newProperty("seataPool").apply { hikari.isAutoCommit = false }
        val result = creator.createDataSource(property)

        assertEquals(true, property.hikari.isAutoCommit,
            "Seata AT mode requires autoCommit=true; the yml setting must be overridden")
        assertEquals(true, property.druid.defaultAutoCommit)
        assertIs<ItemDataSource>(result)
        assertSame(product, result.realDataSource)
        assertSame(proxied, result.dataSource, "target is the Seata proxy")
        assertEquals(true, result.seata)
        assertEquals(SeataMode.AT, result.seataMode)
    }

    @Test
    fun createDataSource_p6spyEnabledOnlyWhenBothGlobalAndPropertyAgree() {
        val creator = DsDataSourceCreator().apply {
            setCreators(listOf(supportingCreator(HikariDataSource())))
            setP6spy(true)
        }
        val enabled = creator.createDataSource(newProperty().apply { p6spy = true }) as ItemDataSource
        assertEquals(true, enabled.p6spy)

        val disabledByProperty = creator.createDataSource(newProperty().apply { p6spy = false }) as ItemDataSource
        assertEquals(false, disabledByProperty.p6spy)

        val globallyOff = DsDataSourceCreator().apply {
            setCreators(listOf(supportingCreator(HikariDataSource())))
            setP6spy(false)
        }
        val disabledGlobally = globallyOff.createDataSource(newProperty().apply { p6spy = true }) as ItemDataSource
        assertEquals(false, disabledGlobally.p6spy)
    }

    @Test
    fun nonSeataProxy_stillWrapsButDoesNotForceAutoCommit() {
        val product = HikariDataSource()
        val proxied = HikariDataSource()
        val plainProxy = object : IDataSourceProxy {
            override fun proxyDatasource(dataSource: DataSource): DataSource = proxied
        }
        val creator = DsDataSourceCreator().apply { setCreators(listOf(supportingCreator(product))) }
        inject(creator, "dataSourceProxy", plainProxy)

        val property = newProperty("plainProxyPool").apply { hikari.isAutoCommit = false }
        val result = creator.createDataSource(property) as ItemDataSource

        assertEquals(false, property.hikari.isAutoCommit, "non-Seata proxies must not touch autoCommit")
        assertSame(proxied, result.dataSource)
        assertFalse(result.seata)
        assertNull(result.seataMode)
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
}
