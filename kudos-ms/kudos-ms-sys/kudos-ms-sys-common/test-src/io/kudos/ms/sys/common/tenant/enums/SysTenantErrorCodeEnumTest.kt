package io.kudos.ms.sys.common.tenant.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for SysTenantErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix, the
 * IErrorCodeEnum.displayText contract and valueOf round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        SysTenantErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, SysTenantErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        SysTenantErrorCodeEnum.entries.forEach { entry ->
            assertEquals("sys.error-msg.tenant", entry.i18nKeyPrefix)
            assertEquals("sys.error-msg.tenant.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Unspecified error", SysTenantErrorCodeEnum.UNSPECIFIED.defaultDisplayText)
        assertEquals("Tenant does not exist", SysTenantErrorCodeEnum.TENANT_NOT_FOUND.defaultDisplayText)
        assertEquals(
            "Tenant name already exists",
            SysTenantErrorCodeEnum.TENANT_NAME_ALREADY_EXISTS.defaultDisplayText
        )
        assertEquals(
            SysTenantErrorCodeEnum.TENANT_NOT_FOUND,
            SysTenantErrorCodeEnum.valueOf("TENANT_NOT_FOUND")
        )
    }

}
