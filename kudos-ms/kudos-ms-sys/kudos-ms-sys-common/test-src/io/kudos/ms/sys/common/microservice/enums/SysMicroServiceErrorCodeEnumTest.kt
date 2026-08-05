package io.kudos.ms.sys.common.microservice.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for SysMicroServiceErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix, the
 * IErrorCodeEnum.displayText contract and valueOf round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysMicroServiceErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        SysMicroServiceErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, SysMicroServiceErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        SysMicroServiceErrorCodeEnum.entries.forEach { entry ->
            assertEquals("sys.error-msg.microservice", entry.i18nKeyPrefix)
            assertEquals("sys.error-msg.microservice.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Microservice does not exist", SysMicroServiceErrorCodeEnum.MICRO_SERVICE_NOT_FOUND.defaultDisplayText)
        assertEquals(
            SysMicroServiceErrorCodeEnum.MICRO_SERVICE_CODE_ALREADY_EXISTS,
            SysMicroServiceErrorCodeEnum.valueOf("MICRO_SERVICE_CODE_ALREADY_EXISTS")
        )
    }

}
