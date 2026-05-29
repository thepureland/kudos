package io.kudos.ability.web.swagger.init.properties

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [SwaggerContact].
 *
 * Smoke-level coverage for the data class shape — defaults are all-null (so the `Info.contact`
 * block is degenerate but not corrupt when nothing is configured), and the setters round-trip.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SwaggerContactTest {

    @Test
    fun defaults_areAllNull() {
        val contact = SwaggerContact()
        assertNull(contact.contactName, "name defaults to null so an unconfigured contact does not surface bogus placeholders")
        assertNull(contact.contactUrl)
        assertNull(contact.contactEmail)
    }

    @Test
    fun settersRoundTrip() {
        val contact = SwaggerContact().apply {
            contactName = "Platform Team"
            contactUrl = "https://example.com/team"
            contactEmail = "platform@example.com"
        }
        assertEquals("Platform Team", contact.contactName)
        assertEquals("https://example.com/team", contact.contactUrl)
        assertEquals("platform@example.com", contact.contactEmail)
    }
}
