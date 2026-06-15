package io.kudos.ms.auth.core.platform.enums.dict

import io.kudos.base.enums.ienums.IModuleEnum
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Pure unit test for [AuthModuleEnum] — a deliberately empty placeholder enum that exists only to
 * satisfy the IModuleEnum SPI contract.
 *
 * Exercises the compiler-generated enum members (`entries`, `values()`, `valueOf`) so the placeholder
 * is provably reachable, and pins the contract: it is currently memberless and is an [IModuleEnum].
 * `valueOf` on any name must throw because there are no constants.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthModuleEnumTest {

    @Test
    fun enumIsEmptyPlaceholder() {
        assertTrue(AuthModuleEnum.entries.isEmpty(), "AuthModuleEnum is a placeholder and must have no members")
        assertTrue(AuthModuleEnum.values().isEmpty())
    }

    @Test
    fun valueOf_anyName_throwsBecauseNoConstants() {
        assertFailsWith<IllegalArgumentException> { AuthModuleEnum.valueOf("ANYTHING") }
    }

    @Test
    fun isAModuleEnumSpiType() {
        assertTrue(IModuleEnum::class.java.isAssignableFrom(AuthModuleEnum::class.java))
    }
}
