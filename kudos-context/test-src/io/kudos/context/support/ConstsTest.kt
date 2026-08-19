package io.kudos.context.support

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * test for Consts
 *
 * Pins down the public constant values: these literals travel across processes (cache keys,
 * request headers), so any accidental change must fail loudly here.
 *
 * @author K
 * @since 1.0.0
 */
internal class ConstsTest {

    @Test
    fun cacheKeyDelimiter() {
        assertNotNull(Consts)
        assertEquals("::", Consts.CACHE_KEY_DEFAULT_DELIMITER)
    }

    @Test
    fun sysDefaultCodes() {
        assertNotNull(Consts.Sys)
        assertEquals("default", Consts.Sys.DEFAULT_PORTAL_CODE)
        assertEquals("default", Consts.Sys.DEFAULT_SUBSYSTEM_CODE)
        assertEquals("default", Consts.Sys.DEFAULT_MICRO_SERVICE_CODE)
    }

    @Test
    fun requestHeaderKeys() {
        assertNotNull(Consts.RequestHeader)
        assertEquals("_rpc_request", Consts.RequestHeader.RPC_REQUEST)
        assertEquals("_notify_request", Consts.RequestHeader.NOTIFY_REQUEST)
        assertEquals("_sub_sys_code", Consts.RequestHeader.SUB_SYS_CODE)
        assertEquals("_tenant_id", Consts.RequestHeader.TENANT_ID)
        assertEquals("_UUID", Consts.RequestHeader.TRACE_KEY)
        assertEquals("_LOCAL", Consts.RequestHeader.LOCAL)
        assertEquals("_DATA_SOURCE_ID", Consts.RequestHeader.DATASOURCE_ID)
        // The cross-process header keys all share the "_" prefix convention
        listOf(
            Consts.RequestHeader.RPC_REQUEST, Consts.RequestHeader.NOTIFY_REQUEST,
            Consts.RequestHeader.SUB_SYS_CODE, Consts.RequestHeader.TENANT_ID,
            Consts.RequestHeader.TRACE_KEY, Consts.RequestHeader.LOCAL,
            Consts.RequestHeader.DATASOURCE_ID
        ).forEach { assertTrue(it.startsWith("_"), "header key must start with '_': $it") }
    }
}
