package io.kudos.ms.sys.common.domain.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for SysDomainErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix, the
 * IErrorCodeEnum.displayText contract and valueOf round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDomainErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        SysDomainErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, SysDomainErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        SysDomainErrorCodeEnum.entries.forEach { entry ->
            assertEquals("sys.error-msg.domain", entry.i18nKeyPrefix)
            assertEquals("sys.error-msg.domain.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Domain not found", SysDomainErrorCodeEnum.DOMAIN_NOT_FOUND.defaultDisplayText)
        assertEquals(
            SysDomainErrorCodeEnum.DOMAIN_ALREADY_EXISTS,
            SysDomainErrorCodeEnum.valueOf("DOMAIN_ALREADY_EXISTS")
        )
    }

}
