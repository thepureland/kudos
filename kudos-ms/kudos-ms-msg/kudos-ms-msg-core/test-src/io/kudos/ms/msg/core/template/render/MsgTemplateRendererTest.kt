package io.kudos.ms.msg.core.template.render

import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * 纯 JVM 单测 —— 渲染器只读模板对象、不依赖容器，足够覆盖。
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
        // year 同名 → 业务方传入的应胜出
        val t = template(title = "你好 \${user}, \${year}", content = "今天 \${date}")
        val r = renderer.render(t, mapOf("user" to "Alice", "year" to "9999"), nowProvider)

        assertEquals("你好 Alice, 9999", r.title)
        assertEquals("今天 2026-05-18", r.content)
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
        val t = template(title = "", content = null, defaultTitle = "默认标题", defaultContent = "默认正文")
        val r = renderer.render(t, emptyMap(), nowProvider)

        assertEquals("默认标题", r.title)
        assertEquals("默认正文", r.content)
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
        val t = template(title = "纯文本", content = "no placeholders here")
        val r = renderer.render(t, mapOf("user" to "ignored"), nowProvider)

        assertEquals("纯文本", r.title)
        assertEquals("no placeholders here", r.content)
    }

    @Test
    fun leaves_unknown_placeholder_unsubstituted() {
        // 没传 fooBar 的话保留原样，让调用方/QA 能从输出里看出哪些占位符漏了
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
