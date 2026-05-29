package io.kudos.ability.web.swagger.init.properties

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [SwaggerProperties].
 *
 * Locks in the default behavior that the auto-configuration relies on:
 *  - `enabled` defaults to `true` so adding the module dependency is enough to publish OpenAPI
 *    without further yml.
 *  - `production` defaults to `false` so dev / staging deployments don't accidentally tag
 *    themselves as production.
 *  - The remaining string fields default to `null` rather than blank — empty strings would
 *    render as empty `<title></title>` in the document, which is uglier than just omitting it.
 *  - `contact` is instantiated (not null) so callers can always navigate
 *    `properties.contact.contactName` without an NPE check, even when nothing is configured.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SwaggerPropertiesTest {

    @Test
    fun defaults_enabledTrueProductionFalse() {
        val props = SwaggerProperties()
        assertTrue(props.enabled, "enabled must default to true so the module activates without extra yml")
        assertEquals(false, props.production)
    }

    @Test
    fun defaults_stringFieldsAreNull() {
        val props = SwaggerProperties()
        assertNull(props.title)
        assertNull(props.description)
        assertNull(props.url)
        assertNull(props.version)
        assertNull(props.groupName)
    }

    @Test
    fun defaults_contactIsInstantiatedNotNull() {
        val props = SwaggerProperties()
        assertNotNull(props.contact, "contact must be eagerly instantiated so nested access does not NPE")
        assertNull(props.contact.contactName)
    }

    @Test
    fun contactSetter_replacesInstance() {
        val props = SwaggerProperties()
        val custom = SwaggerContact().apply { contactName = "Alice" }
        props.contact = custom
        assertSame(custom, props.contact)
        assertEquals("Alice", props.contact.contactName)
    }
}
