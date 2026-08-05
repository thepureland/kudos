package io.kudos.ability.data.rdb.jdbc.datasource

import com.zaxxer.hikari.HikariDataSource
import io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Unit tests for the extension-point defaults: [DefaultDynamicDataSourceLoad] (placeholder
 * loader), [IDataSourceProxy]'s default methods (no-proxy semantics), and the [DatasourceConst]
 * literal contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class DatasourceExtensionDefaultsTest {

    @Test
    fun defaultDynamicDataSourceLoad_alwaysReturnsNull() {
        val load = DefaultDynamicDataSourceLoad()
        assertNull(load.getPropertyById("any-id"))
        assertNull(load.getPropertyById(null))
        assertNull(load.getPropertyById(""))
    }

    @Test
    fun dataSourceProxy_defaultsMeanNoProxy() {
        val proxy = object : IDataSourceProxy {} // pure defaults
        val ds: DataSource = HikariDataSource()
        assertSame(ds, proxy.proxyDatasource(ds), "default proxy is the identity function")
        assertFalse(proxy.isSeata())
        assertNull(proxy.seataMode())
    }

    /**
     * The Kotlin compiler also emits static compatibility bridges in
     * `IDataSourceProxy$DefaultImpls` (jvm-default compatibility mode). Java implementors and
     * older Kotlin binaries dispatch through these bridges, so verify they honour the same
     * "no proxy" contract as the interface default methods.
     */
    @Test
    fun dataSourceProxy_defaultImplsBridges_keepNoProxySemantics() {
        val proxy = object : IDataSourceProxy {}
        val ds: DataSource = HikariDataSource()
        val defaultImpls = Class.forName(
            "io.kudos.ability.data.rdb.jdbc.datasource.IDataSourceProxy\$DefaultImpls"
        )
        val proxyDatasource = defaultImpls.getMethod(
            "proxyDatasource", IDataSourceProxy::class.java, DataSource::class.java
        )
        assertSame(ds, proxyDatasource.invoke(null, proxy, ds), "bridge must also be the identity function")
        val isSeata = defaultImpls.getMethod("isSeata", IDataSourceProxy::class.java)
        assertEquals(false, isSeata.invoke(null, proxy))
        val seataMode = defaultImpls.getMethod("seataMode", IDataSourceProxy::class.java)
        assertNull(seataMode.invoke(null, proxy))
    }

    @Test
    fun datasourceConst_literals() {
        assertEquals("master", DatasourceConst.MODE_MASTER)
        assertEquals("readonly", DatasourceConst.MODE_READONLY)
        assertEquals("-99", DatasourceConst.CONSOLE_TENANT_ID)
        // force-initialize the companion holder so the constant declarations are exercised
        val companion = DatasourceConst::class.java.getDeclaredField("Companion").get(null)
        assertEquals("Companion", companion.javaClass.simpleName)
    }
}
