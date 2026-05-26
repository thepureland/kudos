package io.kudos.base.lang.string

import org.apache.commons.text.StringEscapeUtils

/**
 * String escape utility.
 *
 * @author K
 * @since 1.0.0
 */
object StringEscapeKit {

    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    // Wrapper for org.apache.commons.text.StringEscapeUtils
    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

    //region Java and JavaScript
    /**
     * Escape the specified string using Java string rules.
     * Correctly handles quotes and control characters (tab, backslash, carriage return, form feed, etc.).
     * A tab character will be escaped to `'\\'` and `'t'`.
     * The only difference between Java and JavaScript string rules is that in JavaScript single quotes and slashes are escaped.
     *
     * For example:
     * <pre>
     * input string : He didn't say, "Stop!"
     * output string: He didn't say, \"Stop!\"
     * </pre>
     *
     * @param input the string to escape, may be null
     * @return the escaped string, returns `null` if `null` is passed in
     * @author K
     * @since 1.0.0
     */
    fun escapeJava(input: String?): String? = StringEscapeUtils.escapeJava(input)

    /**
     * Escape the specified string using EcmaScript string rules.
     * Correctly handles quotes and control characters (tab, backslash, carriage return, form feed, etc.).
     * A tab character will be escaped to `'\\'` and `'t'`.
     * The only difference between Java and JavaScript string rules is that in JavaScript single quotes and slashes are escaped.
     * The most famous dialects of EcmaScript are JavaScript and ActionScript.
     *
     * For example:
     * <pre>
     * input string : He didn't say, "Stop!"
     * output string: He didn't say, \"Stop!\"
     * </pre>
     *
     * @param input the string to escape, may be null
     * @return the escaped string, returns `null` if `null` is passed in
     * @author K
     * @since 1.0.0
     */
    fun escapeEcmaScript(input: String?): String? = StringEscapeUtils.escapeEcmaScript(input)

    /**
     * Unescape the specified escaped Java string.
     * For example, converts `'\'` and `'n'` to a line feed, unless the `'\'` is preceded by another `'\'`.
     *
     * @param input the string to unescape, may be null
     * @return a new unescaped `String`, returns `null` if `null` is passed in
     * @author K
     * @since 1.0.0
     */
    fun unescapeJava(input: String?): String? = StringEscapeUtils.unescapeJava(input)

    /**
     * Unescape the specified escaped EcmaScript string.
     * For example, converts `'\'` and `'n'` to a line feed, unless the `'\'` is preceded by another `'\'`.
     *
     * @see .unescapeJava
     * @param input the string to unescape, may be null
     * @return a new unescaped `String`, returns `null` if `null` is passed in
     * @author K
     * @since 1.0.0
     */
    fun unescapeEcmaScript(input: String?): String? = StringEscapeUtils.unescapeEcmaScript(input)
    //endregion Java and JavaScript

    //region HTML and XML
    /**
     * Escape the specified string using HTML entities.
     *
     * For example:
     * `"bread" & "butter"`
     * becomes:
     * `&quot;bread&quot; &amp; &quot;butter&quot;`.
     * Supports all HTML 4.0 entities. Note that the commonly used (&amp;apos;) is not a logical entity and is therefore not supported.
     *
     * @param input the `String` to escape, may be null
     * @return a new escaped string, returns `null` if `null` is passed in
     * @see [ISO Entities](http://hotwired.lycos.com/webmonkey/reference/special_characters/)
     * @see [HTML 3.2 Character Entities for ISO Latin-1](http://www.w3.org/TR/REC-html32.latin1)
     * @see [HTML 4.0 Character entity references](http://www.w3.org/TR/REC-html40/sgml/entities.html)
     * @see [HTML 4.01 Character References](http://www.w3.org/TR/html401/charset.html.h-5.3)
     * @see [HTML 4.01 Code positions](http://www.w3.org/TR/html401/charset.html.code-position)
     * @author K
     * @since 1.0.0
     */
    fun escapeHtml4(input: String?): String? = StringEscapeUtils.escapeHtml4(input)

    /**
     * Escape the specified string using HTML entities.
     * Supports only HTML 3.0 entities.
     *
     * @param input the `String` to escape, may be null
     * @return a new escaped string, returns `null` if `null` is passed in
     * @author K
     * @since 1.0.0
     */
    fun escapeHtml3(input: String?): String? = StringEscapeUtils.escapeHtml3(input)

    /**
     * Unescape the specified escaped HTML 4.0 entity string.
     * For example, the string "&amp;lt;Fran&amp;ccedil;ais&amp;gt;" will be converted to "&lt;Franais&gt;".
     * If an entity is not recognized, it will be left as-is and inserted verbatim into the result. For example: "&amp;gt;&amp;zzzz;x" will be converted to "&gt;&amp;zzzz;x".
     *
     * @param input the string to unescape, may be null
     * @return a new unescaped `String`, returns `null` if `null` is passed in
     * @author K
     * @since 1.0.0
     */
    fun unescapeHtml4(input: String?): String? = StringEscapeUtils.unescapeHtml4(input)

    /**
     * Unescape the specified escaped HTML 3.0 entity string.
     *
     * @param input the string to unescape, may be null
     * @return a new unescaped `String`, returns `null` if `null` is passed in
     * @author K
     * @since 1.0.0
     */
    fun unescapeHtml3(input: String?): String? = StringEscapeUtils.unescapeHtml3(input)

    /**
     * Escape the specified string using XML entities.
     * For example: <tt>"bread" & "butter"</tt> => <tt>&amp;quot;bread&amp;quot; &amp;amp; &amp;quot;butter&amp;quot;</tt>.
     * Supports only the 5 basic XML entities (gt, lt, quot, amp, apos). DTDs and external entities are not supported.
     * Note that Unicode characters greater than 0x7f are not escaped. If you want them escaped, you can do:
     * `StringEscapeUtils.ESCAPE_XML.with( NumericEntityEscaper.between(0x7f, Integer.MAX_VALUE) );`
     *
     * @param input the `String` to escape, may be null
     * @return a new escaped string, returns `null` if `null` is passed in
     * @see .unescapeXml
     * @author K
     * @since 1.0.0
     */
    fun escapeXml10(input: String?): String? = StringEscapeUtils.escapeXml10(input)

    /**
     * Escape the specified string using XML entities.
     * For example: <tt>"bread" & "butter"</tt> => <tt>&amp;quot;bread&amp;quot; &amp;amp; &amp;quot;butter&amp;quot;</tt>.
     * Supports only the 5 basic XML entities (gt, lt, quot, amp, apos). DTDs and external entities are not supported.
     * Note that Unicode characters greater than 0x7f are not escaped. If you want them escaped, you can do:
     * `StringEscapeUtils.ESCAPE_XML.with( NumericEntityEscaper.between(0x7f, Integer.MAX_VALUE) );`
     *
     * @param input the `String` to escape, may be null
     * @return a new escaped string, returns `null` if `null` is passed in
     * @see .unescapeXml
     * @author K
     * @since 1.0.0
     */
    fun escapeXml11(input: String?): String? = StringEscapeUtils.escapeXml11(input)

    /**
     * Unescape the specified escaped XML entity string.
     * Supports only the 5 basic XML entities (gt, lt, quot, amp, apos). DTDs and external entities are not supported.
     * Note: numeric \\u Unicode encodings are not decoded into their corresponding Unicode characters. This may be resolved in a future version.
     *
     * @param input the string to unescape, may be null
     * @return a new unescaped `String`, returns `null` if `null` is passed in
     * @see .escapeXml
     * @author K
     * @since 1.0.0
     */
    fun unescapeXml(input: String?): String? = StringEscapeUtils.unescapeXml(input)
    //endregion HTML and XML

    /**
     * Wrap a CSV field in double quotes if required.
     * If the value contains a comma, newline or double quote, the string value will be wrapped in double quotes.
     * Any double quote characters within the value are escaped with an additional double quote.
     * If the value does not contain a comma, newline or double quote, the string is returned unchanged.
     * See [Wikipedia](http://en.wikipedia.org/wiki/Comma-separated_values) and [RFC 4180](http://tools.ietf.org/html/rfc4180).
     *
     * @param input the value of a CSV column, may be null
     * @return the escaped string, returns null if `null` is passed in
     * @author K
     * @since 1.0.0
     */
    fun escapeCsv(input: String?): String? = StringEscapeUtils.escapeCsv(input)

    /**
     * Unescape an escaped CSV column value.
     * If the value is wrapped in double quotes and contains a comma, newline or double quote, the wrapping double quotes are removed.
     * Two consecutive double quotes are collapsed into one.
     * If the value is not wrapped in double quotes, or is wrapped but does not contain a comma, newline or double quote, the string is returned unchanged.
     * See [Wikipedia](http://en.wikipedia.org/wiki/Comma-separated_values) and [RFC 4180](http://tools.ietf.org/html/rfc4180).
     *
     * @param input the value of a CSV column, may be null
     * @return the unescaped value of the CSV column
     * @author K
     * @since 1.0.0
     */
    fun unescapeCsv(input: String?): String? = StringEscapeUtils.unescapeCsv(input)

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // Wrapper for org.apache.commons.text.StringEscapeUtils
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

}
