package io.kudos.base.lang.string

import kotlin.test.Test
import kotlin.test.assertEquals


internal class EncodeKitTest {

    @Test
    fun testEncodeHex() {
        val input = "haha,i am a very long message"
        val result = EncodeKit.encodeHex(input.toByteArray())
        assertEquals(input, String(EncodeKit.decodeHex(result)))
    }

    @Test
    fun testEncodeBase64() {
        val input = "haha,i am a very long message"
        val result = EncodeKit.encodeBase64(input.toByteArray())
        assertEquals(input, String(EncodeKit.decodeBase64(result)))
    }

    @Test
    fun testEncodeUrlSafeBase64() {
        val input = "haha,i am a very long message"
        val result = EncodeKit.encodeUrlSafeBase64(input.toByteArray())
        assertEquals(input, String(EncodeKit.decodeBase64(result)))
    }

    @Test
    fun testUrlEncode() {
        val input = "http://locahost/?q=中文&t=1"
        val result = EncodeKit.urlEncode(input)
        println(result)
        assertEquals(input, EncodeKit.urlDecode(result))
    }

    @Test
    fun testDecodeHex() {
    }

    @Test
    fun testDecodeBase64() {
    }

    @Test
    fun testEncodeBase62() {
    }

    @Test
    fun testUrlDecode() {
    }
}