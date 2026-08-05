package io.kudos.ms.sys.core.datasource.service

import io.kudos.ms.sys.common.datasource.consts.SysDataSourceConsts
import io.kudos.ms.sys.core.datasource.service.impl.shouldKeepStoredPassword
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure unit test for the "blank or masked password means keep the stored one" update policy
 * (the internal `shouldKeepStoredPassword` function); no Spring context or database required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysDataSourcePasswordPolicyTest {

    @Test
    fun nullBlankOrMaskedPasswordKeepsStoredOne() {
        assertTrue(shouldKeepStoredPassword(null))
        assertTrue(shouldKeepStoredPassword(""))
        assertTrue(shouldKeepStoredPassword("   "))
        assertTrue(shouldKeepStoredPassword(SysDataSourceConsts.PASSWORD_MASK))
    }

    @Test
    fun realPasswordValueIsApplied() {
        assertFalse(shouldKeepStoredPassword("s3cret"))
        assertFalse(shouldKeepStoredPassword("┼AES-ciphertext"))
        // near-miss variants of the mask must count as real values
        assertFalse(shouldKeepStoredPassword("*****"))
        assertFalse(shouldKeepStoredPassword("*******"))
    }
}
