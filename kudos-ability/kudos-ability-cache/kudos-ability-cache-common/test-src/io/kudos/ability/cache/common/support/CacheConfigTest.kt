package io.kudos.ability.cache.common.support

import io.kudos.ability.cache.common.enums.CacheStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [CacheConfig] 派生属性的单测。
 *
 * 守护 README 已声明的契约："新代码必须用派生属性 `resolvedStrategy{Code}` /
 * `isActive` / `isWriteOnBoot` / `isWriteInTime`，不要直接读裸字段"。
 *
 * 覆盖：
 *  - **来源优先级**：yml/代码 `strategy` 胜过 DB 字典码 `strategyDictCode`（README 描述）
 *  - **`resolvedStrategy` enum 解析**：合法值返回 `CacheStrategy`，非法值返回 null（不抛）
 *  - **`isActive` 三态**：null = true（默认初始为 true，反序列化可能拿到 null），false = false
 *  - **`isWriteOnBoot` / `isWriteInTime`**：null = false
 */
internal class CacheConfigTest {

    @Test
    fun resolvedStrategyCode_prefersStrategyOverDictCode() {
        val cfg = CacheConfig().apply {
            strategy = "REMOTE"
            strategyDictCode = "LOCAL_REMOTE"
        }
        assertEquals("REMOTE", cfg.resolvedStrategyCode, "strategy 优先于 strategyDictCode")
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
                "合法 enum 字符串应当解析成对应 CacheStrategy ($code)")
        }
    }

    @Test
    fun resolvedStrategy_returnsNullForUnknownValue_doesNotThrow() {
        val cfg = CacheConfig().apply { strategy = "EXOTIC_STRATEGY" }
        // 非法值返回 null 而非 IllegalArgumentException
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
        assertTrue(cfg.isActive, "默认初始 active=true → isActive=true")
    }

    @Test
    fun isActive_explicitFalse() {
        val cfg = CacheConfig().apply { active = false }
        assertEquals(false, cfg.isActive)
    }

    @Test
    fun isActive_nullTreatedAsTrue() {
        // 反序列化场景：active 字段可能为 null。README 契约：null 视为 true
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
