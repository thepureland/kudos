package io.kudos.context.core

import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * test for ClientInfo
 *
 * Coverage:
 * - All Builder chained setters return the same builder instance (fluent API)
 * - build() copies every field from the builder to the ClientInfo
 * - Unset fields stay null (default semantics)
 * - null can be explicitly assigned through every setter
 * - Unicode / boundary values survive the copy untouched
 *
 * @author K
 * @since 1.0.0
 */
internal class ClientInfoTest {

    @Test
    fun builderSettersReturnSameBuilderForChaining() {
        val builder = ClientInfo.Builder()
        assertSame(builder, builder.ip("1.2.3.4"))
        assertSame(builder, builder.domain("example.com"))
        assertSame(builder, builder.url("/index"))
        assertSame(builder, builder.params(emptyMap()))
        assertSame(builder, builder.requestContentString("body"))
        assertSame(builder, builder.requestReferer("ref"))
        assertSame(builder, builder.requestType("GET"))
        assertSame(builder, builder.os("Windows" to "11"))
        assertSame(builder, builder.browser("Chrome" to "120"))
        assertSame(builder, builder.locale(Locale.CHINA))
        assertSame(builder, builder.timeZone(TimeZone.getTimeZone("Asia/Shanghai")))
    }

    @Test
    fun buildCopiesAllFieldsFromBuilder() {
        val params: Map<String, Array<String?>?> = mapOf(
            "a" to arrayOf("1", null),
            "empty" to null
        )
        val locale = Locale.forLanguageTag("zh-TW")
        val tz = TimeZone.getTimeZone("UTC")
        val info = ClientInfo.Builder()
            .ip("10.0.0.1")
            .domain("kudos.io")
            .url("https://kudos.io/login?x=中文")
            .params(params)
            .requestContentString("{\"key\":\"值\"}")
            .requestReferer("https://referer.example")
            .requestType("POST")
            .os("Linux" to "6.1")
            .browser("Firefox" to "121")
            .locale(locale)
            .timeZone(tz)
            .build()

        assertEquals("10.0.0.1", info.ip)
        assertEquals("kudos.io", info.domain)
        assertEquals("https://kudos.io/login?x=中文", info.url)
        assertSame(params, info.params)
        assertEquals("{\"key\":\"值\"}", info.requestContentString)
        assertEquals("https://referer.example", info.requestReferer)
        assertEquals("POST", info.requestType)
        assertEquals("Linux" to "6.1", info.os)
        assertEquals("Firefox" to "121", info.browser)
        assertSame(locale, info.locale)
        assertSame(tz, info.timeZone)
    }

    @Test
    fun unsetFieldsDefaultToNull() {
        val info = ClientInfo.Builder().build()
        assertNull(info.ip)
        assertNull(info.domain)
        assertNull(info.url)
        assertNull(info.params)
        assertNull(info.requestContentString)
        assertNull(info.requestReferer)
        assertNull(info.requestType)
        assertNull(info.os)
        assertNull(info.browser)
        assertNull(info.locale)
        assertNull(info.timeZone)
    }

    @Test
    fun nullCanBeExplicitlyAssignedThroughEverySetter() {
        val info = ClientInfo.Builder()
            .ip(null).domain(null).url(null).params(null)
            .requestContentString(null).requestReferer(null).requestType(null)
            .os(null).browser(null).locale(null).timeZone(null)
            .build()
        assertNull(info.ip)
        assertNull(info.timeZone)
    }

    @Test
    fun fieldsAreMutableAfterBuild() {
        val info = ClientInfo.Builder().ip("1.1.1.1").build()
        info.ip = "2.2.2.2"
        assertEquals("2.2.2.2", info.ip)
    }
}
