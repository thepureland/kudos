package io.kudos.ms.user.core.login.cache

import io.kudos.context.support.Consts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Pure-logic test for [RememberMeByTenantIdAndUsernameCache] — the parts reachable without a Spring
 * container or database: the [RememberMeByTenantIdAndUsernameCache.getKey] composition (with trimming)
 * and the `doReload` key-format validation guards. The full cache pipeline (reload / sync / DB
 * round-trips) is covered by the container-backed [RememberMeByTenantIdAndUsernameCacheTest].
 *
 * `doReload` is `protected`; a same-package subclass exposes it. The validation branches throw before
 * any Spring/DAO access, so a directly instantiated handler is safe here.
 *
 * @author K
 * @since 1.0.0
 */
internal class RememberMeCachePureTest {

    private class TestableCache : RememberMeByTenantIdAndUsernameCache() {
        fun callDoReload(key: String) = doReload(key)
    }

    private val delim = Consts.CACHE_KEY_DEFAULT_DELIMITER

    @Test
    fun getKey_composesTenantAndUsernameWithDelimiter() {
        val cache = RememberMeByTenantIdAndUsernameCache()
        assertEquals("t1${delim}alice", cache.getKey("t1", "alice"))
    }

    @Test
    fun getKey_trimsBothParts() {
        val cache = RememberMeByTenantIdAndUsernameCache()
        assertEquals("t1${delim}alice", cache.getKey("  t1  ", "  alice  "))
    }

    @Test
    fun doReload_keyWithoutDelimiter_throwsIllegalArgument() {
        val cache = TestableCache()
        val ex = assertFailsWith<IllegalArgumentException> { cache.callDoReload("no-delimiter-here") }
        assertTrue(ex.message!!.contains("tenantId"))
    }

    @Test
    fun doReload_keyWithTooManyParts_throwsIllegalArgument() {
        val cache = TestableCache()
        // 3 segments -> split size 3 != 2 -> require(parts.size == 2) fails
        assertFailsWith<IllegalArgumentException> {
            cache.callDoReload("a${delim}b${delim}c")
        }
    }
}
