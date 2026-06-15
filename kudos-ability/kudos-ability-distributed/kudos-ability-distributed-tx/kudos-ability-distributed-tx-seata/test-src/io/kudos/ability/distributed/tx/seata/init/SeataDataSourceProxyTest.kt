package io.kudos.ability.distributed.tx.seata.init

import com.baomidou.dynamic.datasource.enums.SeataMode
import java.io.PrintWriter
import java.sql.Connection
import java.sql.SQLException
import java.util.logging.Logger
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for [SeataDataSourceProxy].
 *
 * Coverage:
 * - proxyDatasource(): proxy disabled returns the original DataSource untouched
 * - proxyDatasource(): unrecognised / null proxy mode raises IllegalArgumentException listing valid values
 * - proxyDatasource(): AT / XA branches are entered (case-insensitively); when the underlying
 *   DataSource fails, the failure is wrapped into IllegalArgumentException("Failed to proxy DataSource")
 * - isSeata(): always true
 * - seataMode(): AT / XA (case-insensitive) / unknown / null mappings
 *
 * @author K
 * @since 1.0.0
 */
internal class SeataDataSourceProxyTest {

    @Test
    fun proxyDatasource_returnsOriginalDataSourceWhenProxyDisabled() {
        val proxy = SeataDataSourceProxy().apply {
            setPrivateField("proxyMode", "AT")
            setPrivateField("enableProxy", false)
        }

        assertSame(NoopDataSource, proxy.proxyDatasource(NoopDataSource))
    }

    @Test
    fun proxyDatasource_invalidProxyModeListsAllowedValues() {
        val proxy = SeataDataSourceProxy().apply {
            setPrivateField("proxyMode", "INVALID")
            setPrivateField("enableProxy", true)
        }

        val exception = assertFailsWith<IllegalArgumentException> {
            proxy.proxyDatasource(NoopDataSource)
        }

        val message = exception.message.orEmpty()
        assertTrue("seata.data-source-proxy-mode" in message)
        assertTrue("INVALID" in message)
        assertTrue("AT | XA" in message)
    }

    @Test
    fun proxyDatasource_nullProxyModeIsRejectedAsInvalid() {
        val proxy = SeataDataSourceProxy().apply {
            setPrivateField("proxyMode", null)
            setPrivateField("enableProxy", true)
        }

        val exception = assertFailsWith<IllegalArgumentException> {
            proxy.proxyDatasource(NoopDataSource)
        }
        assertTrue("'null'" in exception.message.orEmpty())
    }

    @Test
    fun proxyDatasource_atModeWrapsUnderlyingFailureIntoIllegalArgumentException() {
        // "at" lower-case also proves the mode match is case-insensitive.
        val proxy = SeataDataSourceProxy().apply {
            setPrivateField("proxyMode", "at")
            setPrivateField("enableProxy", true)
        }

        val exception = assertFailsWith<IllegalArgumentException> {
            proxy.proxyDatasource(FailingDataSource)
        }
        assertEquals("Failed to proxy DataSource", exception.message)
        assertNotNull(exception.cause)
    }

    @Test
    fun proxyDatasource_xaModeWrapsUnderlyingFailureIntoIllegalArgumentException() {
        val proxy = SeataDataSourceProxy().apply {
            setPrivateField("proxyMode", "xA")
            setPrivateField("enableProxy", true)
        }

        val exception = assertFailsWith<IllegalArgumentException> {
            proxy.proxyDatasource(FailingDataSource)
        }
        assertEquals("Failed to proxy DataSource", exception.message)
        assertNotNull(exception.cause)
    }

    @Test
    fun isSeata_alwaysTrue() {
        assertTrue(SeataDataSourceProxy().isSeata())
    }

    @Test
    fun seataMode_mapsAtCaseInsensitively() {
        assertEquals(SeataMode.AT, proxyWithMode("AT").seataMode())
        assertEquals(SeataMode.AT, proxyWithMode("at").seataMode())
    }

    @Test
    fun seataMode_mapsXaCaseInsensitively() {
        assertEquals(SeataMode.XA, proxyWithMode("XA").seataMode())
        assertEquals(SeataMode.XA, proxyWithMode("xa").seataMode())
    }

    @Test
    fun seataMode_unknownOrNullModeReturnsNull() {
        assertNull(proxyWithMode("INVALID").seataMode())
        assertNull(proxyWithMode("").seataMode())
        assertNull(proxyWithMode(null).seataMode())
    }

    private fun proxyWithMode(mode: String?): SeataDataSourceProxy =
        SeataDataSourceProxy().apply { setPrivateField("proxyMode", mode) }

    private fun Any.setPrivateField(name: String, value: Any?) {
        val field = this::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(this, value)
    }

    private object NoopDataSource : DataSource {
        override fun getConnection(): Connection = error("not used")
        override fun getConnection(username: String?, password: String?): Connection = error("not used")
        override fun getLogWriter(): PrintWriter? = null
        override fun setLogWriter(out: PrintWriter?) {}
        override fun setLoginTimeout(seconds: Int) {}
        override fun getLoginTimeout(): Int = 0
        override fun getParentLogger(): Logger = Logger.getGlobal()
        override fun <T : Any?> unwrap(iface: Class<T>?): T = error("not used")
        override fun isWrapperFor(iface: Class<*>?): Boolean = false
    }

    /** A DataSource whose connections always fail, to drive Seata proxy constructors into their error path. */
    private object FailingDataSource : DataSource {
        override fun getConnection(): Connection = throw SQLException("connection refused (test stub)")
        override fun getConnection(username: String?, password: String?): Connection =
            throw SQLException("connection refused (test stub)")
        override fun getLogWriter(): PrintWriter? = null
        override fun setLogWriter(out: PrintWriter?) {}
        override fun setLoginTimeout(seconds: Int) {}
        override fun getLoginTimeout(): Int = 0
        override fun getParentLogger(): Logger = Logger.getGlobal()
        override fun <T : Any?> unwrap(iface: Class<T>?): T = error("not used")
        override fun isWrapperFor(iface: Class<*>?): Boolean = false
    }

}
