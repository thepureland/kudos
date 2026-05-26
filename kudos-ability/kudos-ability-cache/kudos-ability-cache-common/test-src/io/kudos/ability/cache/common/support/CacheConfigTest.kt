package io.kudos.ability.cache.common.support

import io.kudos.ability.cache.common.enums.CacheStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [CacheConfig] derived properties.
 *
 * Guards the contract documented in the README: "new code must use the derived properties
 * `resolvedStrategy{Code}` / `isActive` / `isWriteOnBoot` / `isWriteInTime`, and must not read the raw fields directly".
 *
 * Coverage:
 *  - **Source precedence**: yml/code `strategy` wins over the DB dict code `strategyDictCode` (README).
 *  - **`resolvedStrategy` enum parsing**: valid values return a `CacheStrategy`; invalid values return null (no throw).
 *  - **`isActive` three states**: null = true (default initial is true; deserialization may yield null), false = false.
 *  - **`isWriteOnBoot` / `isWriteInTime`**: null = false.
 */
internal class CacheConfigTest {

    @Test
    fun resolvedStrategyCode_prefersStrategyOverDictCode() {
        val cfg = CacheConfig().apply {
            strategy = "REMOTE"
            strategyDictCode = "LOCAL_REMOTE"
        }
        assertEquals("REMOTE", cfg.resolvedStrategyCode, "strategy takes precedence over strategyDictCode")
    }

    @Test
    fun resolvedStrategyCode_fallsBackToDictCode_whenStrategyMissing() {
        val cfg = CacheConfig().apply {
            strategyDictCode = "SINGLE_LOCAL"
        }
        assertEquals("SINGLE_LOCAL", cfg.resolvedStrategyCode)
    }

    @Test
    fun resolvedStrategyCode_bothNull_returnsNull() {
        val cfg = CacheConfig()
        assertNull(cfg.resolvedStrategyCode)
    }

    @Test
    fun resolvedStrategy_parsesValidEnumValues() {
        listOf("SINGLE_LOCAL", "REMOTE", "LOCAL_REMOTE").forEach { code ->
            val cfg = CacheConfig().apply { strategy = code }
            assertEquals(CacheStrategy.valueOf(code), cfg.resolvedStrategy,
                "A valid enum string should parse to the corresponding CacheStrategy ($code)")
        }
    }

    @Test
    fun resolvedStrategy_returnsNullForUnknownValue_doesNotThrow() {
        val cfg = CacheConfig().apply { strategy = "EXOTIC_STRATEGY" }
        // Invalid values return null instead of throwing IllegalArgumentException.
        assertNull(cfg.resolvedStrategy)
    }

    @Test
    fun resolvedStrategy_returnsNullWhenBothCodesNull() {
        val cfg = CacheConfig()
        assertNull(cfg.resolvedStrategy)
    }

    @Test
    fun isActive_defaultTrue() {
        val cfg = CacheConfig()
        assertTrue(cfg.isActive, "Default initial active=true → isActive=true")
    }

    @Test
    fun isActive_explicitFalse() {
        val cfg = CacheConfig().apply { active = false }
        assertEquals(false, cfg.isActive)
    }

    @Test
    fun isActive_nullTreatedAsTrue() {
        // Deserialization scenario: the active field may be null. README contract: null is treated as true.
        val cfg = CacheConfig().apply { active = null }
        assertTrue(cfg.isActive)
    }

    @Test
    fun isWriteOnBoot_nullTreatedAsFalse() {
        val cfg = CacheConfig().apply { writeOnBoot = null }
        assertEquals(false, cfg.isWriteOnBoot)
    }

    @Test
    fun isWriteOnBoot_explicitTrue() {
        val cfg = CacheConfig().apply { writeOnBoot = true }
        assertTrue(cfg.isWriteOnBoot)
    }

    @Test
    fun isWriteInTime_nullTreatedAsFalse() {
        val cfg = CacheConfig().apply { writeInTime = null }
        assertEquals(false, cfg.isWriteInTime)
    }

    @Test
    fun constructor_setsAllExplicitFields() {
        val cfg = CacheConfig(
            name = "USER",
            strategyDictCode = "REMOTE",
            writeOnBoot = true,
            writeInTime = false,
            ttl = 600,
            active = true,
        )
        assertEquals("USER", cfg.name)
        assertEquals("REMOTE", cfg.strategyDictCode)
        assertTrue(cfg.isWriteOnBoot)
        assertEquals(false, cfg.isWriteInTime)
        assertEquals(600, cfg.ttl)
        assertTrue(cfg.isActive)
    }
}
