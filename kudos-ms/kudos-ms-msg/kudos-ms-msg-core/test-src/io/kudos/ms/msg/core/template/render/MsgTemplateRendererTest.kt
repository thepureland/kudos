package io.kudos.ms.msg.core.template.render

import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * Pure JVM unit test — the renderer only reads template objects and does not depend on the container, which is sufficient coverage.
 *
 * @author K
 * @since 1.0.0
 */
class MsgTemplateRendererTest {

    private val renderer = MsgTemplateRenderer()

    private val frozen = LocalDateTime.of(2026, 5, 18, 9, 30, 45)
    private val nowProvider: () -> LocalDateTime = { frozen }

    @Test
    fun renders_business_params_overriding_auto_params() {
        // year name conflict -> the business-provided value should win
        val t = template(title = "Hello \${user}, \${year}", content = "Today \${date}")
        val r = renderer.render(t, mapOf("user" to "Alice", "year" to "9999"), nowProvider)

        assertEquals("Hello Alice, 9999", r.title)
        assertEquals("Today 2026-05-18", r.content)
        assertEquals("9999", r.paramsUsed["year"])
        assertEquals("Alice", r.paramsUsed["user"])
    }

    @Test
    fun fills_auto_params_when_missing() {
        val t = template(title = "[\${time}]", content = "\${year}/\${month}/\${day}")
        val r = renderer.render(t, emptyMap(), nowProvider)

        assertEquals("[2026-05-18 09:30:45]", r.title)
        assertEquals("2026/05/18", r.content)
    }

    @Test
    fun falls_back_to_defaults_when_main_blank() {
        val t = template(title = "", content = null, defaultTitle = "Default title", defaultContent = "Default body")
        val r = renderer.render(t, emptyMap(), nowProvider)

        assertEquals("Default title", r.title)
        assertEquals("Default body", r.content)
    }

    @Test
    fun returns_empty_strings_when_no_main_or_default() {
        val t = template(title = null, content = null, defaultTitle = null, defaultContent = null)
        val r = renderer.render(t, emptyMap(), nowProvider)

        assertEquals("", r.title)
        assertEquals("", r.content)
    }

    @Test
    fun handles_template_without_placeholders() {
        val t = template(title = "Plain text", content = "no placeholders here")
        val r = renderer.render(t, mapOf("user" to "ignored"), nowProvider)

        assertEquals("Plain text", r.title)
        assertEquals("no placeholders here", r.content)
    }

    @Test
    fun leaves_unknown_placeholder_unsubstituted() {
        // If fooBar is not provided, keep it as-is so the caller/QA can see which placeholders were missed in the output
        val t = template(title = "\${fooBar}", content = "")
        val r = renderer.render(t, emptyMap(), nowProvider)
        assertTrue(r.title.contains("\${fooBar}"), "missing placeholders should remain visible, was: ${r.title}")
    }

    private fun template(
        title: String? = null,
        content: String? = null,
        defaultTitle: String? = null,
        defaultContent: String? = null,
    ) = MsgTemplateCacheEntry(
        id = "t-1",
        sendTypeDictCode = null,
        eventTypeDictCode = null,
        msgTypeDictCode = null,
        receiverGroupCode = null,
        localeDictCode = null,
        title = title,
        content = content,
        defaultActive = true,
        defaultTitle = defaultTitle,
        defaultContent = defaultContent,
        tenantId = "tenant-1",
    )
}
