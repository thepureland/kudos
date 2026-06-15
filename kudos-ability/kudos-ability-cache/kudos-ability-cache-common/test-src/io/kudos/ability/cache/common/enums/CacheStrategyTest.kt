package io.kudos.ability.cache.common.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Unit tests for [CacheStrategy].
 *
 * Coverage:
 *  - The enum exposes exactly the three documented constants in declaration order.
 *  - `valueOf` round-trips each constant name and throws on an unknown name (the parse contract that
 *    [io.kudos.ability.cache.common.support.CacheConfig.resolvedStrategy] guards against).
 *
 * @author K
 * @since 1.0.0
 */
internal class CacheStrategyTest {

    @Test
    fun entries_areExactlyTheThreeStrategies_inOrder() {
        assertEquals(
            listOf("SINGLE_LOCAL", "REMOTE", "LOCAL_REMOTE"),
            CacheStrategy.entries.map { it.name }
        )
    }

    @Test
    fun valueOf_roundTripsEachConstant() {
        CacheStrategy.entries.forEach {
            assertEquals(it, CacheStrategy.valueOf(it.name))
        }
    }

    @Test
    fun valueOf_unknown_throwsIllegalArgument() {
        assertFailsWith<IllegalArgumentException> { CacheStrategy.valueOf("NOPE") }
    }
}
