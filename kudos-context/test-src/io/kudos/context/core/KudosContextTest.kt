package io.kudos.context.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * test for KudosContext
 *
 * Coverage:
 * - All plain properties default to null and are settable
 * - addSessionAttributes / addCookieAttributes / addHeaderAttributes / addOtherInfos:
 *   lazy creation of the underlying map on first call, appending on subsequent calls,
 *   overwriting on duplicate keys, null values, empty varargs, and chained returns
 * - Companion constants
 *
 * @author K
 * @since 1.0.0
 */
internal class KudosContextTest {

    @Test
    fun allPropertiesDefaultToNull() {
        val ctx = KudosContext()
        assertNull(ctx.portalCode)
        assertNull(ctx.subSystemCode)
        assertNull(ctx.microServiceCode)
        assertNull(ctx.atomicServiceCode)
        assertNull(ctx.tenantId)
        assertNull(ctx.dataSourceId)
        assertNull(ctx.readOnlyDataSourceId)
        assertNull(ctx._datasourceTenantId)
        assertNull(ctx.user)
        assertNull(ctx.traceKey)
        assertNull(ctx.clientInfo)
        assertNull(ctx.sessionAttributes)
        assertNull(ctx.cookieAttributes)
        assertNull(ctx.headerAttributes)
        assertNull(ctx.otherInfos)
    }

    @Test
    fun plainPropertiesAreSettable() {
        val ctx = KudosContext().apply {
            portalCode = "p"
            subSystemCode = "s"
            microServiceCode = "m"
            atomicServiceCode = "a"
            tenantId = "t"
            dataSourceId = "ds"
            readOnlyDataSourceId = "ro"
            _datasourceTenantId = "dst"
            traceKey = "trace-1"
            clientInfo = ClientInfo.Builder().ip("1.1.1.1").build()
        }
        assertEquals("p", ctx.portalCode)
        assertEquals("s", ctx.subSystemCode)
        assertEquals("m", ctx.microServiceCode)
        assertEquals("a", ctx.atomicServiceCode)
        assertEquals("t", ctx.tenantId)
        assertEquals("ds", ctx.dataSourceId)
        assertEquals("ro", ctx.readOnlyDataSourceId)
        assertEquals("dst", ctx._datasourceTenantId)
        assertEquals("trace-1", ctx.traceKey)
        assertEquals("1.1.1.1", ctx.clientInfo?.ip)
    }

    // ============================================================
    // addSessionAttributes
    // ============================================================

    @Test
    fun addSessionAttributesLazilyCreatesMapAndAppends() {
        val ctx = KudosContext()
        assertNull(ctx.sessionAttributes, "map should not exist before the first call")

        val returned = ctx.addSessionAttributes("k1" to "v1", "k2" to 2)
        assertSame(ctx, returned, "should return itself for chaining")
        assertEquals(mapOf<String, Any?>("k1" to "v1", "k2" to 2), ctx.sessionAttributes!!.toMap())

        val firstMap = ctx.sessionAttributes
        ctx.addSessionAttributes("k3" to null)
        assertSame(firstMap, ctx.sessionAttributes, "the second call should reuse the existing map")
        assertEquals(3, ctx.sessionAttributes!!.size)
        assertNull(ctx.sessionAttributes!!["k3"], "null value should be storable")
    }

    @Test
    fun addSessionAttributesOverwritesDuplicateKey() {
        val ctx = KudosContext().addSessionAttributes("k" to "old").addSessionAttributes("k" to "new")
        assertEquals("new", ctx.sessionAttributes!!["k"])
        assertEquals(1, ctx.sessionAttributes!!.size)
    }

    @Test
    fun addSessionAttributesWithEmptyVarargsStillCreatesMap() {
        val ctx = KudosContext().addSessionAttributes()
        assertNotNull(ctx.sessionAttributes)
        assertEquals(0, ctx.sessionAttributes!!.size)
    }

    // ============================================================
    // addCookieAttributes
    // ============================================================

    @Test
    fun addCookieAttributesLazilyCreatesMapAndAppends() {
        val ctx = KudosContext()
        assertNull(ctx.cookieAttributes)
        assertSame(ctx, ctx.addCookieAttributes("c1" to "v1"))
        ctx.addCookieAttributes("c2" to null)
        assertEquals(mapOf("c1" to "v1", "c2" to null), ctx.cookieAttributes!!.toMap())
    }

    // ============================================================
    // addHeaderAttributes
    // ============================================================

    @Test
    fun addHeaderAttributesLazilyCreatesMapAndAppends() {
        val ctx = KudosContext()
        assertNull(ctx.headerAttributes)
        assertSame(ctx, ctx.addHeaderAttributes("h1" to "v1"))
        ctx.addHeaderAttributes("h1" to "v2", "h2" to null)
        assertEquals(mapOf("h1" to "v2", "h2" to null), ctx.headerAttributes!!.toMap())
    }

    // ============================================================
    // addOtherInfos
    // ============================================================

    @Test
    fun addOtherInfosLazilyCreatesMapAndAppends() {
        val ctx = KudosContext()
        assertNull(ctx.otherInfos)
        assertSame(ctx, ctx.addOtherInfos(KudosContext.OTHER_INFO_KEY_DATA_SOURCE to "ds-1"))
        ctx.addOtherInfos(KudosContext.OTHER_INFO_KEY_DATABASE to "db-1", "custom" to 42)
        assertEquals("ds-1", ctx.otherInfos!![KudosContext.OTHER_INFO_KEY_DATA_SOURCE])
        assertEquals("db-1", ctx.otherInfos!![KudosContext.OTHER_INFO_KEY_DATABASE])
        assertEquals(42, ctx.otherInfos!!["custom"])
    }

    // ============================================================
    // Companion constants
    // ============================================================

    @Test
    fun companionConstantsHaveExpectedValues() {
        assertNotNull(KudosContext.Companion)
        assertEquals("_DATA_SOURCE_", KudosContext.OTHER_INFO_KEY_DATA_SOURCE)
        assertEquals("_DATABASE_", KudosContext.OTHER_INFO_KEY_DATABASE)
        assertEquals("_VERIFY_CODE_", KudosContext.OTHER_INFO_KEY_VERIFY_CODE)
        assertEquals("_USER_", KudosContext.SESSION_KEY_USER)
    }
}
