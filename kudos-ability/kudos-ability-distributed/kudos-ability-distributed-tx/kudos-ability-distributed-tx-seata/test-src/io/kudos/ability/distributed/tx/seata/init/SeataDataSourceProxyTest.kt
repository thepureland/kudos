package io.kudos.ability.distributed.tx.seata.init

import java.io.PrintWriter
import java.sql.Connection
import java.util.logging.Logger
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue


internal class SeataDataSourceProxyTest {

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
        assertTrue("AT | XA" in message)
    }

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

}
