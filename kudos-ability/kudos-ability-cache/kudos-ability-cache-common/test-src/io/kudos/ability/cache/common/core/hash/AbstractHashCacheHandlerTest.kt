package io.kudos.ability.cache.common.core.hash

import io.kudos.ability.cache.common.aop.hash.FakeHashCache
import io.kudos.ability.cache.common.aop.hash.HashEntity
import io.kudos.ability.cache.common.aop.hash.ProgrammableCacheConfigProvider
import io.kudos.ability.cache.common.aop.hash.buildHashManager
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [AbstractHashCacheHandler]: reload (delete + optional write-back via doReload), evict,
 * the inactive-cache short-circuit, and the default secondary-property accessors / exposedEntityClass.
 *
 * @author K
 * @since 1.0.0
 */
internal class AbstractHashCacheHandlerTest {

    private val cacheName = "HASH_HANDLER_TEST"
    private lateinit var fake: FakeHashCache
    private lateinit var provider: ProgrammableCacheConfigProvider

    @BeforeTest
    fun setup() {
        fake = FakeHashCache()
        provider = ProgrammableCacheConfigProvider()
        val manager = buildHashManager(cacheName to fake)
        HashCacheKit.overrideForTesting(manager, provider)
    }

    @AfterTest
    fun teardown() {
        HashCacheKit.resetForTesting()
    }

    @Test
    fun reload_inactiveCache_doesNothing() {
        provider.register(cacheName, active = false)
        fake.save(cacheName, HashEntity("1"))
        val handler = newHandler()
        handler.reload("1")
        // still present, doReload not consulted
        assertTrue(fake.entities.containsKey("1"))
        assertEquals(0, handler.doReloadCalls)
    }

    @Test
    fun reload_deletesThenWritesBackWhenDoReloadReturnsEntity() {
        provider.register(cacheName, active = true)
        fake.save(cacheName, HashEntity("1", type = "old"))
        val handler = newHandler(reloadResult = HashEntity("1", type = "new"))
        handler.reload("1")
        assertEquals("new", (fake.entities["1"] as HashEntity).type)
        assertEquals(1, handler.doReloadCalls)
        // saved back with the secondary-property sets
        assertEquals(setOf("type"), fake.saveBatchCalls.single().second)
        assertEquals(setOf("score"), fake.saveBatchCalls.single().third)
    }

    @Test
    fun reload_deletesAndDoesNotWriteWhenDoReloadReturnsNull() {
        provider.register(cacheName, active = true)
        fake.save(cacheName, HashEntity("1"))
        val handler = newHandler(reloadResult = null)
        handler.reload("1")
        assertFalse(fake.entities.containsKey("1"))
        assertTrue(fake.saveBatchCalls.isEmpty())
    }

    @Test
    fun evict_inactiveCache_doesNothing() {
        provider.register(cacheName, active = false)
        fake.save(cacheName, HashEntity("1"))
        newHandler().evict("1")
        assertTrue(fake.entities.containsKey("1"))
    }

    @Test
    fun evict_active_deletesEntry() {
        provider.register(cacheName, active = true)
        fake.save(cacheName, HashEntity("1"))
        newHandler().evict("1")
        assertFalse(fake.entities.containsKey("1"))
    }

    @Test
    fun exposedEntityClass_returnsEntityClass() {
        val handler = newHandler()
        assertEquals(HashEntity::class, handler.exposedEntityClass())
    }

    @Test
    fun baseDoReload_returnsNull_deletesOnly() {
        // a handler that overrides NOTHING beyond the abstract members -> base doReload(id)=null is used,
        // so reload deletes without write-back.
        provider.register(cacheName, active = true)
        fake.save(cacheName, HashEntity("1"))
        val handler = object : AbstractHashCacheHandler<HashEntity>() {
            override fun cacheName(): String = cacheName
            override fun reloadAll(clear: Boolean) {}
            override fun entityClass(): KClass<HashEntity> = HashEntity::class
        }
        handler.reload("1")
        assertFalse(fake.entities.containsKey("1"))
        assertTrue(fake.saveBatchCalls.isEmpty())
    }

    @Test
    fun baseDefaultSecondaryProperties_areEmpty() {
        // A handler that does NOT override the secondary-property accessors -> base returns empty sets,
        // exercised through reload's write-back path.
        provider.register(cacheName, active = true)
        fake.save(cacheName, HashEntity("1"))
        val handler = object : AbstractHashCacheHandler<HashEntity>() {
            override fun cacheName(): String = cacheName
            override fun reloadAll(clear: Boolean) {}
            override fun entityClass(): KClass<HashEntity> = HashEntity::class
            override fun doReload(id: Any): HashEntity? = HashEntity("1")
        }
        handler.reload("1")
        assertEquals(emptySet(), fake.saveBatchCalls.single().second)
        assertEquals(emptySet(), fake.saveBatchCalls.single().third)
    }

    // ---- helpers ----

    private fun newHandler(reloadResult: HashEntity? = null): TestHashHandler {
        return TestHashHandler(cacheName, reloadResult)
    }

    private class TestHashHandler(
        private val name: String,
        private val reloadResult: HashEntity?,
    ) : AbstractHashCacheHandler<HashEntity>() {
        var doReloadCalls = 0
        override fun cacheName(): String = name
        override fun reloadAll(clear: Boolean) {}
        override fun entityClass(): KClass<HashEntity> = HashEntity::class
        override fun filterableProperties(): Set<String> = setOf("type")
        override fun sortableProperties(): Set<String> = setOf("score")
        override fun doReload(id: Any): HashEntity? {
            doReloadCalls++
            return reloadResult
        }
    }
}
