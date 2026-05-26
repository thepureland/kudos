package io.kudos.base.lang.string

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


/**
 * test for StringEscapeKit
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class StringEscapeKitTest {

    //region Java and JavaScript string escaping

    @Test
    fun escapeJava_NullInput_ReturnsNull() {
        assertNull(StringEscapeKit.escapeJava(null))
    }

    @Test
    fun escapeJava_SimpleString_EscapesQuotesAndControlChars() {
        val input = "He didn't say,\n\"Stop!\"\tOK"
        val escaped = StringEscapeKit.escapeJava(input)
        // single quotes are not escaped; newlines become \n; double quotes and backslashes are escaped; tabs become \t
        assertEquals("He didn't say,\\n\\\"Stop!\\\"\\tOK", escaped)
    }

    @Test
    fun unescapeJava_NullInput_ReturnsNull() {
        assertNull(StringEscapeKit.unescapeJava(null))
    }

    @Test
    fun unescapeJava_EscapedString_UnescapesCorrectly() {
        val escaped = "Line1\\nLine2\\tTabbed\\\\Backslash\\\"Quote"
        val unescaped = StringEscapeKit.unescapeJava(escaped)
        assertEquals("Line1\nLine2\tTabbed\\Backslash\"Quote", unescaped)
    }

    @Test
    fun escapeEcmaScript_NullInput_ReturnsNull() {
        assertNull(StringEscapeKit.escapeEcmaScript(null))
    }

    @Test
    fun escapeEcmaScript_SimpleString_EscapesForJavaScript() {
        val input = "He didn't say,\n/Stop!/"
        val escaped = StringEscapeKit.escapeEcmaScript(input)
        // In EcmaScript single quotes, slashes, and newlines are all escaped
        assertEquals("He didn\\'t say,\\n\\/Stop!\\/", escaped)
    }

    @Test
    fun unescapeEcmaScript_NullInput_ReturnsNull() {
        assertNull(StringEscapeKit.unescapeEcmaScript(null))
    }

    @Test
    fun unescapeEcmaScript_EscapedString_UnescapesCorrectly() {
        val escaped = "Hello\\nWorld\\'Test\\\"Slash\\\\"
        val unescaped = StringEscapeKit.unescapeEcmaScript(escaped)
        assertEquals("Hello\nWorld'Test\"Slash\\", unescaped)
    }

    //endregion

    //region HTML escaping

    @Test
    fun escapeHtml4_NullInput_ReturnsNull() {
        assertNull(StringEscapeKit.escapeHtml4(null))
    }

    @Test
    fun escapeHtml4_Entities_EscapesCorrectly() {
        val input = "\"bread\" & \"butter\" <tag>"
        val escaped = StringEscapeKit.escapeHtml4(input)
        // double quote -> &quot;, & -> &amp;, < -> &lt;, > -> &gt;
        assertEquals("&quot;bread&quot; &amp; &quot;butter&quot; &lt;tag&gt;", escaped)
    }

    @Test
    fun unescapeHtml4_NullInput_ReturnsNull() {
        assertNull(StringEscapeKit.unescapeHtml4(null))
    }

    @Test
    fun unescapeHtml4_Entities_UnescapesCorrectly() {
        val escaped = "&lt;Français&gt; &amp; &quot;Quotes&quot;"
        val unescaped = StringEscapeKit.unescapeHtml4(escaped)
        assertEquals("<Français> & \"Quotes\"", unescaped)
    }

    @Test
    fun escapeHtml3_NullInput_ReturnsNull() {
        assertNull(StringEscapeKit.escapeHtml3(null))
    }

    @Test
    fun escapeHtml3_BasicEntities_EscapesCorrectly() {
        val input = "<p>3 > 2 & 1 < 4</p>"
        val escaped = StringEscapeKit.escapeHtml3(input)
        // HTML3 supports &lt;, &gt;, &amp;
        assertEquals("&lt;p&gt;3 &gt; 2 &amp; 1 &lt; 4&lt;/p&gt;", escaped)
    }

    @Test
    fun unescapeHtml3_NullInput_ReturnsNull() {
        assertNull(StringEscapeKit.unescapeHtml3(null))
    }

    @Test
    fun unescapeHtml3_Entities_UnescapesCorrectly() {
        val escaped = "&lt;tag&gt;Text &amp; More&lt;/tag&gt;"
        val unescaped = StringEscapeKit.unescapeHtml3(escaped)
        assertEquals("<tag>Text & More</tag>", unescaped)
    }

    //endregion

    //region XML escaping

    @Test
    fun escapeXml10_NullInput_ReturnsNull() {
        assertNull(StringEscapeKit.escapeXml10(null))
    }

    @Test
    fun escapeXml10_BasicEntities_EscapesCorrectly() {
        val input = "\"bread\" & \"butter\" <xml>"
        val escaped = StringEscapeKit.escapeXml10(input)
        // XML10 supports &quot;, &amp;, &lt;, &gt;
        assertEquals("&quot;bread&quot; &amp; &quot;butter&quot; &lt;xml&gt;", escaped)
    }

    @Test
    fun escapeXml11_NullInput_ReturnsNull() {
        assertNull(StringEscapeKit.escapeXml11(null))
    }

    @Test
    fun escapeXml11_BasicEntities_EscapesCorrectly() {
        val input = "'data' & <node>"
        val escaped = StringEscapeKit.escapeXml11(input)
        // XML11 supports &apos;, &amp;, &lt;, &gt;
        assertEquals("&apos;data&apos; &amp; &lt;node&gt;", escaped)
    }

    @Test
    fun unescapeXml_NullInput_ReturnsNull() {
        assertNull(StringEscapeKit.unescapeXml(null))
    }

    @Test
    fun unescapeXml_Entities_UnescapesCorrectly() {
        val escaped = "&lt;item&gt;Fish &amp; Chips&lt;/item&gt;"
        val unescaped = StringEscapeKit.unescapeXml(escaped)
        assertEquals("<item>Fish & Chips</item>", unescaped)
    }

    //endregion

    //region CSV escaping

    @Test
    fun escapeCsv_NullInput_ReturnsNull() {
        assertNull(StringEscapeKit.escapeCsv(null))
    }

    @Test
    fun escapeCsv_NoSpecialChars_ReturnsSame() {
        val input = "simpleValue"
        val escaped = StringEscapeKit.escapeCsv(input)
        assertEquals("simpleValue", escaped)
    }

    @Test
    fun escapeCsv_ContainsCommaOrQuote_EscapesProperly() {
        val input = "He said, \"Hello, World\""
        val escaped = StringEscapeKit.escapeCsv(input)
        // When a value contains a comma or double quote, the whole field is wrapped in double quotes and inner quotes are doubled
        assertEquals("\"He said, \"\"Hello, World\"\"\"", escaped)
    }

    @Test
    fun escapeCsv_ContainsNewline_EscapesProperly() {
        val input = "Line1\nLine2"
        val escaped = StringEscapeKit.escapeCsv(input)
        assertEquals("\"Line1\nLine2\"", escaped)
    }

    @Test
    fun unescapeCsv_NullInput_ReturnsNull() {
        assertNull(StringEscapeKit.unescapeCsv(null))
    }

    @Test
    fun unescapeCsv_QuotedValue_UnescapesProperly() {
        // Directly use a quoted CSV field that contains inner double quotes
        val field = "\"He said, \"\"Hi\"\"\""
        val unescaped = StringEscapeKit.unescapeCsv(field)
        assertEquals("He said, \"Hi\"", unescaped)
    }

    @Test
    fun unescapeCsv_NoQuotes_ReturnsSame() {
        val input = "plain,field"
        val unescaped = StringEscapeKit.unescapeCsv(input)
        assertEquals("plain,field", unescaped)
    }

    @Test
    fun unescapeCsv_EmbeddedEscapedQuotes() {
        // Input string: "\"\"\"\"\"\"" (six double quotes), which after decoding represents two double quotes
        val input = "\"\"\"\"\"\""
        val unescaped = StringEscapeKit.unescapeCsv(input)
        assertEquals("\"\"", unescaped)
    }

    //endregion

}