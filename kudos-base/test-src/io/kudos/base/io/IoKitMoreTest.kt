package io.kudos.base.io

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.StringReader
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Additional [IoKit] test cases covering overloads and default-argument (`encoding = null`) variants that
 * [IoKitTest] exercises only with an explicit encoding, plus the remaining copy/read/skip overloads.
 *
 * @author K
 * @since 1.0.0
 */
internal class IoKitMoreTest {

    @Test
    fun toByteArray_reader_defaultEncoding() {
        val bytes = IoKit.toByteArray(StringReader("plain"))
        assertEquals("plain", bytes.toString(Charsets.UTF_8))
    }

    @Test
    fun toCharArray_inputStream_defaultEncoding() {
        val chars = IoKit.toCharArray(ByteArrayInputStream("cd".toByteArray()))
        assertEquals("cd", String(chars))
    }

    @Test
    fun toString_defaultEncoding_variants() {
        assertEquals("a", IoKit.toString(ByteArrayInputStream("a".toByteArray())))
        assertEquals("b", IoKit.toString("b".toByteArray()))
        val tmp = kotlin.io.path.createTempFile(suffix = ".txt")
        tmp.toFile().writeText("u")
        assertEquals("u", IoKit.toString(tmp.toUri()))
        assertEquals("u", IoKit.toString(tmp.toUri().toURL()))
    }

    @Test
    fun readLines_inputStream_defaultEncoding() {
        val lines = IoKit.readLines(ByteArrayInputStream("x\ny".toByteArray()))
        assertEquals(listOf("x", "y"), lines)
    }

    @Test
    fun lineIterator_inputStream_defaultEncoding() {
        val it = IoKit.lineIterator(ByteArrayInputStream("p\nq".toByteArray()))
        val collected = buildList { while (it.hasNext()) add(it.next()) }
        assertEquals(listOf("p", "q"), collected)
        it.close()
    }

    @Test
    fun toInputStream_defaultEncoding() {
        assertEquals("cs", IoKit.toInputStream("cs" as CharSequence).readBytes().toString(Charsets.UTF_8))
        assertEquals("s", IoKit.toInputStream("s").readBytes().toString(Charsets.UTF_8))
    }

    @Test
    fun write_byteArray_writer_defaultEncoding() {
        val sw = StringWriter()
        IoKit.write("hi".toByteArray(), sw)
        assertEquals("hi", sw.toString())

        val baos = ByteArrayOutputStream()
        IoKit.write("yo".toCharArray(), baos) // CharArray -> OutputStream, default encoding
        assertEquals("yo", baos.toString(Charsets.UTF_8.name()))

        val baos2 = ByteArrayOutputStream()
        IoKit.write("seq" as CharSequence, baos2) // CharSequence -> OutputStream, default encoding
        assertEquals("seq", baos2.toString(Charsets.UTF_8.name()))

        val baos3 = ByteArrayOutputStream()
        IoKit.write("str", baos3) // String -> OutputStream, default encoding
        assertEquals("str", baos3.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun writeLines_defaultLineEndingAndEncoding() {
        val baos = ByteArrayOutputStream()
        IoKit.writeLines(listOf("a", "b"), output = baos)
        assertTrue(baos.toString(Charsets.UTF_8.name()).contains("a"))

        val sw = StringWriter()
        IoKit.writeLines(listOf("c", "d"), writer = sw)
        assertTrue(sw.toString().contains("c"))
    }

    @Test
    fun copyLarge_inputStream_offsetAndLength() {
        // copyLarge(InputStream, OutputStream, inputOffset, length)
        val out = ByteArrayOutputStream()
        IoKit.copyLarge(ByteArrayInputStream("abcdef".toByteArray()), out, 2L, 3L)
        assertEquals("cde", out.toString(Charsets.UTF_8.name()))

        // copyLarge(InputStream, OutputStream, inputOffset, length, buffer)
        val out2 = ByteArrayOutputStream()
        IoKit.copyLarge(ByteArrayInputStream("abcdef".toByteArray()), out2, 1L, 2L, ByteArray(2))
        assertEquals("bc", out2.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun copy_inputStream_writer_defaultEncoding() {
        val sw = StringWriter()
        IoKit.copy(ByteArrayInputStream("z".toByteArray()), sw)
        assertEquals("z", sw.toString())
    }

    @Test
    fun copyLarge_reader_offsetAndLength() {
        val sw = StringWriter()
        IoKit.copyLarge(StringReader("abcdef"), sw, 2L, 3L)
        assertEquals("cde", sw.toString())

        val sw2 = StringWriter()
        IoKit.copyLarge(StringReader("abcdef"), sw2, 1L, 2L, CharArray(2))
        assertEquals("bc", sw2.toString())
    }

    @Test
    fun copy_reader_outputStream_defaultEncoding() {
        val baos = ByteArrayOutputStream()
        IoKit.copy(StringReader("w"), baos)
        assertEquals("w", baos.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun skipFully_reader_success_and_eof() {
        val r = StringReader("abcdef")
        IoKit.skipFully(r, 2L)
        val buf = CharArray(4)
        r.read(buf)
        assertEquals("cdef", String(buf))

        assertFailsWith<EOFException> { IoKit.skipFully(StringReader("ab"), 5L) }
    }

    @Test
    fun read_inputStream_buffer_noOffset() {
        val buf = ByteArray(4)
        val n = IoKit.read(ByteArrayInputStream("data".toByteArray()), buf)
        assertEquals(4, n)
        assertEquals("data", String(buf, Charsets.UTF_8))
    }

    @Test
    fun readFully_reader_buffer_noOffset() {
        val buf = CharArray(3)
        IoKit.readFully(StringReader("XYZ"), buf)
        assertEquals("XYZ", String(buf))
    }

    @Test
    fun readFully_inputStream_buffer_noOffset() {
        val buf = ByteArray(2)
        IoKit.readFully(ByteArrayInputStream("hi".toByteArray()), buf)
        assertEquals("hi", String(buf, Charsets.UTF_8))
    }

    @Test
    fun read_reader_buffer_noOffset() {
        val buf = CharArray(2)
        val n = IoKit.read(StringReader("kl"), buf)
        assertEquals(2, n)
        assertEquals("kl", String(buf))
    }
}
