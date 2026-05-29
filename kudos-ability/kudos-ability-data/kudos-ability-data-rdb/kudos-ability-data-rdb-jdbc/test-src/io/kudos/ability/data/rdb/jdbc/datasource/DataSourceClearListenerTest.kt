package io.kudos.ability.data.rdb.jdbc.datasource

import io.kudos.ability.cache.common.support.CacheCleanRegister
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [DataSourceClearListener]. Cover the three key-shape branches and the
 * after-properties-set self-registration. Use a stub [DsContextProcessor] subclass to capture
 * `refreshDatasource` invocations without spinning up a Spring context.
 */
internal class DataSourceClearListenerTest {

    @Test
    fun nullKey_triggersFullRefresh() {
        val processor = RecordingProcessor()
        val listener = DataSourceClearListener(processor, cacheName = "any")

        listener.cleanCache("any", key = null)

        assertEquals(listOf<Int?>(null), processor.calls, "null key means region cleared → refresh all")
    }

    @Test
    fun broadcastKey_isIgnored() {
        val processor = RecordingProcessor()
        val listener = DataSourceClearListener(processor, cacheName = "any")

        listener.cleanCache("any", key = "ALL")

        assertEquals(emptyList(), processor.calls,
            "broadcast key 'ALL' must be ignored — every node would otherwise stampede a refresh on every other node's boot")
    }

    @Test
    fun numericKey_refreshesThatDataSource() {
        val processor = RecordingProcessor()
        val listener = DataSourceClearListener(processor, cacheName = "any")

        listener.cleanCache("any", key = "42")

        assertEquals(listOf<Int?>(42), processor.calls)
    }

    @Test
    fun unparseableKey_isSilentlyIgnored() {
        val processor = RecordingProcessor()
        val listener = DataSourceClearListener(processor, cacheName = "any")

        listener.cleanCache("any", key = "not-an-int")
        listener.cleanCache("any", key = java.util.UUID.randomUUID())

        assertEquals(emptyList(), processor.calls,
            "non-int keys aren't valid datasource ids in DsContextProcessor's contract today; silent skip avoids noisy logs and accidental refreshes")
    }

    @Test
    fun afterPropertiesSet_registersAgainstConfiguredCacheName() {
        val processor = RecordingProcessor()
        val customName = "DS_CACHE_${System.nanoTime()}" // unique per run since CacheCleanRegister is process-wide
        val listener = DataSourceClearListener(processor, cacheName = customName)

        listener.afterPropertiesSet()

        val registered = CacheCleanRegister.getCleanListener(customName)
        assertNotNull(registered, "afterPropertiesSet should populate the registry under the configured cacheName")
        assertTrue(registered.any { it === listener }, "the registered entry should be this listener instance")
    }

    @Test
    fun defaultCacheName_matchesKudosMsSysConvention() {
        // Hard-coded compatibility check: if SysDataSourceHashCache.CACHE_NAME changes upstream,
        // this constant must change here. Avoids silent breakage where the listener subscribes to
        // a cache name no one publishes to.
        assertEquals("SYS_DATA_SOURCE__HASH", DataSourceClearListener.DEFAULT_CACHE_NAME)
        assertEquals("ALL", DataSourceClearListener.BROADCAST_KEY)
    }

    /** Captures refresh calls; subclasses [DsContextProcessor] to avoid mocking framework. */
    private class RecordingProcessor : DsContextProcessor() {
        val calls: MutableList<Int?> = CopyOnWriteArrayList()
        override fun refreshDatasource(dsId: Int?) {
            calls += dsId
        }
    }
}
