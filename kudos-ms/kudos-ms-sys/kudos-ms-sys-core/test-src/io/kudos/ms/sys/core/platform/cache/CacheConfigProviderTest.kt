package io.kudos.ms.sys.core.platform.cache

import io.kudos.ms.sys.core.cache.dao.SysCacheDao
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for CacheConfigProvider
 *
 * Builds a fresh provider instance backed by the real [SysCacheDao] (injected via reflection,
 * bypassing the Spring-managed singleton whose internal config list may already be frozen by startup),
 * so every branch can be asserted deterministically against the seeded rows.
 *
 * Coverage:
 * - getAllCacheConfigs: only active=true rows are returned, keyed by name
 * - getCacheConfig: hit and miss (null)
 * - getLocalCacheConfigs / getRemoteCacheConfigs / getLocalRemoteCacheConfigs: strategy filtering
 * - getHashCacheConfigs: hash=true filtering
 * - lazy in-memory caching: second call hits the memoized list (same instance)
 *
 * Test data source: `CacheConfigProviderTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
internal class CacheConfigProviderTest : RdbTestBase() {

    @Resource
    private lateinit var sysCacheDao: SysCacheDao

    private val localName = "ccp-test-local_9447"
    private val remoteName = "ccp-test-remote_9447"
    private val localRemoteName = "ccp-test-local-remote_9447"
    private val hashName = "ccp-test-hash_9447"
    private val inactiveName = "ccp-test-inactive_9447"

    private fun newProvider(): CacheConfigProvider {
        val provider = CacheConfigProvider()
        val field = CacheConfigProvider::class.java.getDeclaredField("sysCacheDao")
        field.isAccessible = true
        field.set(provider, sysCacheDao)
        return provider
    }

    @Test
    fun getAllCacheConfigs_returnsOnlyActiveRowsKeyedByName() {
        val all = newProvider().getAllCacheConfigs()
        assertTrue(all.containsKey(localName))
        assertTrue(all.containsKey(remoteName))
        assertTrue(all.containsKey(localRemoteName))
        assertTrue(all.containsKey(hashName))
        assertFalse(all.containsKey(inactiveName), "inactive rows must be excluded")
        assertEquals(localName, all[localName]?.name)
    }

    @Test
    fun getCacheConfig_hitAndMiss() {
        val provider = newProvider()
        val config = assertNotNull(provider.getCacheConfig(localName))
        assertEquals("SINGLE_LOCAL", config.strategyDictCode)
        assertEquals(3600, config.ttl)
        assertNull(provider.getCacheConfig("ccp-test-no-such-name_9447"))
    }

    @Test
    fun getLocalCacheConfigs_filtersSingleLocalStrategy() {
        val locals = newProvider().getLocalCacheConfigs()
        assertTrue(locals.containsKey(localName))
        assertFalse(locals.containsKey(remoteName))
        assertFalse(locals.containsKey(localRemoteName))
        assertTrue(locals.values.all { it.strategyDictCode == "SINGLE_LOCAL" })
    }

    @Test
    fun getRemoteCacheConfigs_filtersRemoteStrategy() {
        val remotes = newProvider().getRemoteCacheConfigs()
        assertTrue(remotes.containsKey(remoteName))
        assertTrue(remotes.containsKey(hashName))
        assertFalse(remotes.containsKey(localName))
        assertFalse(remotes.containsKey(localRemoteName))
        assertFalse(remotes.containsKey(inactiveName))
        assertTrue(remotes.values.all { it.strategyDictCode == "REMOTE" })
    }

    @Test
    fun getLocalRemoteCacheConfigs_filtersLocalRemoteStrategy() {
        val localRemotes = newProvider().getLocalRemoteCacheConfigs()
        assertTrue(localRemotes.containsKey(localRemoteName))
        assertFalse(localRemotes.containsKey(localName))
        assertFalse(localRemotes.containsKey(remoteName))
        assertTrue(localRemotes.values.all { it.strategyDictCode == "LOCAL_REMOTE" })
    }

    @Test
    fun getHashCacheConfigs_filtersHashFlag() {
        val hashes = newProvider().getHashCacheConfigs()
        assertTrue(hashes.containsKey(hashName))
        assertFalse(hashes.containsKey(localName))
        assertFalse(hashes.containsKey(remoteName))
        assertTrue(hashes.values.all { it.hash })
    }

    @Test
    fun configsAreMemoizedAfterFirstLoad() {
        val provider = newProvider()
        val first = assertNotNull(provider.getCacheConfig(localName))
        val second = assertNotNull(provider.getCacheConfig(localName))
        assertSame(first, second, "second call must reuse the in-memory cached list")
    }
}
