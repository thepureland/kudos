package io.kudos.base.io

import java.io.*
import java.net.URLConnection
import java.nio.charset.UnsupportedCharsetException
import kotlin.io.path.createTempFile
import kotlin.io.path.readBytes
import kotlin.io.path.writeText
import kotlin.test.*

/**
 * test for IoKit
 *
 * @author ChatGPT
 * @author K
 * @since 1.0.0
 */
class IoKitTest {

    @Test
    fun closeNullConnectionDoesNothing() {
        // 验证关闭空 URLConnection 时不会抛出异常
        IoKit.close(null)
    }

    @Test
    fun toBufferedInputStreamWrapsStream() {
        // toBufferedInputStream 应当将普通 InputStream 包装为 BufferedInputStream 并且能读取全部内容
        val raw = ByteArrayInputStream("hello".toByteArray())
        val buffered = IoKit.toBufferedInputStream(raw)
        assertNotNull(buffered)
        val data = buffered.readBytes()
        assertEquals("hello", data.toString(Charsets.UTF_8))
    }

    @Test
    fun toBufferedReaderNullReturnsNull() {
        assertFailsWith<NullPointerException> {
            IoKit.toBufferedReader(null)
        }
    }

    @Test
    fun toBufferedReaderWrapsReader() {
        // toBufferedReader 应当将普通 Reader 包装为 BufferedReader 并且能按行读取
        val raw = StringReader("line1\nline2")
        val buffered = IoKit.toBufferedReader(raw)
        assertNotNull(buffered)
        val first = buffered.readLine()
        assertEquals("line1", first)
    }

    @Test
    fun toByteArrayFromInputStream() {
        // toByteArray(InputStream) 应当将完整的 InputStream 内容读出为 byte 数组
        val content = "abc123"
        val input = ByteArrayInputStream(content.toByteArray())
        val result = IoKit.toByteArray(input)
        assertEquals(content, result.toString(Charsets.UTF_8))
    }

    @Test
    fun toByteArrayFromInputStreamWithKnownSize() {
        val content = "KotlinTest"
        val bytes = content.toByteArray()

        // toByteArray(InputStream, size: Long) 在 size 正确时返回整个数组
        val input = ByteArrayInputStream(bytes)
        val result = IoKit.toByteArray(input, bytes.size.toLong())
        assertEquals(content, result.toString(Charsets.UTF_8))

        // size 小于真实长度时，应返回截断后的数组（长度为指定 size）
        val input2 = ByteArrayInputStream(bytes)
        val truncated = IoKit.toByteArray(input2, (bytes.size - 2).toLong())
        assertEquals(bytes.size - 2, truncated.size)
        assertEquals(content.substring(0, bytes.size - 2), truncated.toString(Charsets.UTF_8))

        // 负 size 时应抛 IllegalArgumentException
        val input3 = ByteArrayInputStream(bytes)
        assertFailsWith<IllegalArgumentException> {
            IoKit.toByteArray(input3, (-1).toLong())
        }
    }

    @Test
    fun toByteArrayFromInputStreamWithIntSize() {
        val content = "ByteArray"
        val bytes = content.toByteArray()

        // toByteArray(InputStream, size: Int) 在 size 正确时返回数组
        val input = ByteArrayInputStream(bytes)
        val result = IoKit.toByteArray(input, bytes.size)
        assertEquals(content, result.toString(Charsets.UTF_8))

        // 负 size 时抛 IllegalArgumentException
        val input2 = ByteArrayInputStream(bytes)
        assertFailsWith<IllegalArgumentException> {
            IoKit.toByteArray(input2, -5)
        }
    }

    @Test
    fun toByteArrayFromReaderWithEncoding() {
        val content = "测试"
        val reader = StringReader(content)

        // toByteArray(Reader, encoding) 应当返回编码正确的 byte 数组
        val bytes = IoKit.toByteArray(reader, Charsets.UTF_8.name())
        assertEquals(content, bytes.toString(Charsets.UTF_8))

        // 不支持的 encoding 抛 UnsupportedCharsetException
        val reader2 = StringReader(content)
        assertFailsWith<UnsupportedCharsetException> {
            IoKit.toByteArray(reader2, "INVALID_CHARSET")
        }
    }

    @Test
    fun toByteArrayFromUriAndUrlAndURLConnection() {
        // toByteArray(URI)、toByteArray(URL)、toByteArray(URLConnection) 应当从文件 URL 正确读取
        val tmpFile = createTempFile(suffix = ".txt")
        tmpFile.writeText("URI and URL test")

        // 从 URI 读取
        val uri = tmpFile.toUri()
        val fromUri = IoKit.toByteArray(uri)
        assertEquals(tmpFile.readBytes().toString(Charsets.UTF_8), fromUri.toString(Charsets.UTF_8))

        // 从 URL 读取
        val url = tmpFile.toUri().toURL()
        val fromUrl = IoKit.toByteArray(url)
        assertEquals(tmpFile.readBytes().toString(Charsets.UTF_8), fromUrl.toString(Charsets.UTF_8))

        // 从 URLConnection 读取
        val conn: URLConnection = url.openConnection()
        val fromConn = IoKit.toByteArray(conn)
        assertEquals(tmpFile.readBytes().toString(Charsets.UTF_8), fromConn.toString(Charsets.UTF_8))
    }

    @Test
    fun toCharArrayFromInputStreamAndReader() {
        val text = "chars"

        // toCharArray(InputStream, encoding) 应当返回正确的 char[]；不支持编码抛 UnsupportedCharsetException
        val input = ByteArrayInputStream(text.toByteArray())
        val chars1 = IoKit.toCharArray(input, Charsets.UTF_8.name())
        assertEquals(text, String(chars1))

        // toCharArray(Reader) 应当返回正确的 char[]
        val reader = StringReader(text)
        val chars2 = IoKit.toCharArray(reader)
        assertEquals(text, String(chars2))
    }

    @Test
    fun toStringFromVariousSources() {
        val text = "Hello\nWorld"

        // toString(InputStream, encoding)
        val inputStream = ByteArrayInputStream(text.toByteArray(Charsets.UTF_8))
        val str1 = IoKit.toString(inputStream, Charsets.UTF_8.name())
        assertEquals(text, str1)

        // toString(Reader)
        val reader = StringReader(text)
        val str2 = IoKit.toString(reader)
        assertEquals(text, str2)

        // toString(ByteArray, encoding)
        val bytes = text.toByteArray(Charsets.UTF_8)
        val str3 = IoKit.toString(bytes, Charsets.UTF_8.name())
        assertEquals(text, str3)

        // toString(URI, encoding)
        val tmp = createTempFile(suffix = ".txt")
        tmp.writeText(text)
        val uri = tmp.toUri()
        val str4 = IoKit.toString(uri, Charsets.UTF_8.name())
        assertEquals(text, str4)

        // toString(URL, encoding)
        val url = uri.toURL()
        val str5 = IoKit.toString(url, Charsets.UTF_8.name())
        assertEquals(text, str5)
    }

    @Test
    fun toStringUnsupportedEncodingThrows() {
        // toString(InputStream, encoding) 不支持编码应抛 UnsupportedCharsetException
        val input = ByteArrayInputStream("x".toByteArray())
        assertFailsWith<UnsupportedCharsetException> {
            IoKit.toString(input, "INVALID")
        }
    }

    @Test
    fun readLinesFromInputStreamAndReader() {
        val lines = listOf("one", "two", "three")
        val content = lines.joinToString("\n")

        // readLines(InputStream, encoding) 应当返回按行分割的 List<String>
        val input = ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))
        val result1 = IoKit.readLines(input, Charsets.UTF_8.name())
        assertEquals(lines, result1)

        // readLines(Reader) 应当返回按行分割的 List<String>
        val reader = StringReader(content)
        val result2 = IoKit.readLines(reader)
        assertEquals(lines, result2)
    }

    @Test
    fun readLinesNonexistentStreamThrows() {
        // readLines 如果传入一个已经关闭（或未连接）的 PipedInputStream，会抛出 UncheckedIOException
        val input = PipedInputStream()
        input.close()
        assertFailsWith<UncheckedIOException> {
            IoKit.readLines(input, Charsets.UTF_8.name())
        }
    }

    @Test
    fun lineIteratorFromReaderAndInputStream() {
        val lines = listOf("a", "b", "c")
        val content = lines.joinToString("\n")

        // lineIterator(Reader)
        val reader = StringReader(content)
        val it1 = IoKit.lineIterator(reader)
        val collected1 = mutableListOf<String>()
        while (it1.hasNext()) {
            collected1 += it1.next()
        }
        assertEquals(lines, collected1)
        it1.close()

        // lineIterator(InputStream, encoding)
        val input = ByteArrayInputStream(content.toByteArray())
        val it2 = IoKit.lineIterator(input, Charsets.UTF_8.name())
        val collected2 = mutableListOf<String>()
        while (it2.hasNext()) {
            collected2 += it2.next()
        }
        assertEquals(lines, collected2)
        it2.close()
    }

    @Test
    fun toInputStreamFromCharSequenceAndString() {
        // toInputStream(CharSequence, encoding)
        val cs: CharSequence = "sample"
        val is1 = IoKit.toInputStream(cs, Charsets.UTF_8.name())
        val out1 = is1.readBytes().toString(Charsets.UTF_8)
        assertEquals("sample", out1)

        // toInputStream(String, encoding)
        val s = "text"
        val is2 = IoKit.toInputStream(s, Charsets.UTF_8.name())
        val out2 = is2.readBytes().toString(Charsets.UTF_8)
        assertEquals("text", out2)
    }

    @Test
    fun writeByteArrayAndCharArrayAndCharSequenceAndString() {
        // write(ByteArray, OutputStream)
        val dataBytes = "bytes".toByteArray(Charsets.UTF_8)
        val baos = ByteArrayOutputStream()
        IoKit.write(dataBytes, baos)
        assertEquals("bytes", baos.toString(Charsets.UTF_8.name()))

        // write(ByteArray, Writer, encoding)
        val sw = StringWriter()
        IoKit.write(dataBytes, sw, Charsets.UTF_8.name())
        assertEquals("bytes", sw.toString())

        // write(CharArray, Writer)
        val dataChars = "chars".toCharArray()
        val sw2 = StringWriter()
        IoKit.write(dataChars, sw2)
        assertEquals("chars", sw2.toString())

        // write(CharArray, OutputStream, encoding)
        val baos2 = ByteArrayOutputStream()
        IoKit.write(dataChars, baos2, Charsets.UTF_8.name())
        assertEquals("chars", baos2.toString(Charsets.UTF_8.name()))

        // write(CharSequence, Writer)
        val cs: CharSequence = "seq"
        val sw3 = StringWriter()
        IoKit.write(cs, sw3)
        assertEquals("seq", sw3.toString())

        // write(CharSequence, OutputStream, encoding)
        val baos3 = ByteArrayOutputStream()
        IoKit.write(cs, baos3, Charsets.UTF_8.name())
        assertEquals("seq", baos3.toString(Charsets.UTF_8.name()))

        // write(String, Writer)
        val str = "string"
        val sw4 = StringWriter()
        IoKit.write(str, sw4)
        assertEquals("string", sw4.toString())

        // write(String, OutputStream, encoding)
        val baos4 = ByteArrayOutputStream()
        IoKit.write(str, baos4, Charsets.UTF_8.name())
        assertEquals("string", baos4.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun writeLinesToOutputStreamAndWriter() {
        val lines = listOf("l1", "l2", "l3")

        // writeLines(Collection<*>, lineEnding, OutputStream, encoding)
        val baos = ByteArrayOutputStream()
        IoKit.writeLines(lines, "\n", baos, Charsets.UTF_8.name())
        val result = baos.toString(Charsets.UTF_8.name()).split("\n")
        assertEquals(lines, result.filter { it.isNotEmpty() })

        // writeLines(Collection<*>, lineEnding, Writer)
        val sw = StringWriter()
        IoKit.writeLines(lines, "\n", sw)
        val result2 = sw.toString().split("\n")
        assertEquals(lines, result2.filter { it.isNotEmpty() })
    }

    @Test
    fun copyAndCopyLargeBetweenStreamsAndReadersWriters() {
        val text = "copyTestData"

        // copy(InputStream, OutputStream)
        val input = ByteArrayInputStream(text.toByteArray())
        val out = ByteArrayOutputStream()
        val count = IoKit.copy(input, out)
        assertTrue(count > 0)
        assertEquals(text, out.toString(Charsets.UTF_8.name()))

        // copyLarge(InputStream, OutputStream)
        val input2 = ByteArrayInputStream(text.toByteArray())
        val out2 = ByteArrayOutputStream()
        IoKit.copyLarge(input2, out2)
        assertEquals(text, out2.toString(Charsets.UTF_8.name()))

        // copyLarge(InputStream, OutputStream, buffer)
        val input3 = ByteArrayInputStream(text.toByteArray())
        val out3 = ByteArrayOutputStream()
        val buf = ByteArray(4)
        IoKit.copyLarge(input3, out3, buf)
        assertEquals(text, out3.toString(Charsets.UTF_8.name()))

        // copy(InputStream, Writer, encoding)
        val input4 = ByteArrayInputStream(text.toByteArray())
        val sw = StringWriter()
        IoKit.copy(input4, sw, Charsets.UTF_8.name())
        assertEquals(text, sw.toString())

        // copy(Reader, Writer)
        val reader = StringReader(text)
        val sw2 = StringWriter()
        val nChars = IoKit.copy(reader, sw2)
        assertTrue(nChars > 0)
        assertEquals(text, sw2.toString())

        // copyLarge(Reader, Writer)
        val reader2 = StringReader(text)
        val sw3 = StringWriter()
        IoKit.copyLarge(reader2, sw3)
        assertEquals(text, sw3.toString())

        // copyLarge(Reader, Writer, buffer)
        val reader3 = StringReader(text)
        val sw4 = StringWriter()
        val charBuf = CharArray(3)
        IoKit.copyLarge(reader3, sw4, charBuf)
        assertEquals(text, sw4.toString())

        // copy(Reader, OutputStream, encoding)
        val reader4 = StringReader(text)
        val baos = ByteArrayOutputStream()
        IoKit.copy(reader4, baos, Charsets.UTF_8.name())
        assertEquals(text, baos.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun contentEqualsInputStreamAndReader() {
        // contentEquals(InputStream, InputStream)
        val a = ByteArrayInputStream("same".toByteArray())
        val b = ByteArrayInputStream("same".toByteArray())
        assertTrue(IoKit.contentEquals(a, b))

        // InputStream 内容不同
        val a2 = ByteArrayInputStream("one".toByteArray())
        val b2 = ByteArrayInputStream("two".toByteArray())
        assertFalse(IoKit.contentEquals(a2, b2))

        // contentEquals(Reader, Reader)
        val r1 = StringReader("line1\n")
        val r2 = StringReader("line1\n")
        assertTrue(IoKit.contentEquals(r1, r2))

        // Reader 内容不同
        val r3 = StringReader("a\nb")
        val r4 = StringReader("a\nc")
        assertFalse(IoKit.contentEquals(r3, r4))

        // contentEqualsIgnoreEOL(Reader, Reader) 忽略行结束符后相等
        val r5 = StringReader("x\r\ny\n")
        val r6 = StringReader("x\ny\n")
        assertTrue(IoKit.contentEqualsIgnoreEOL(r5, r6))
    }

    @Test
    fun skipAndSkipFullyBehaviors() {
        val data = "abcdefgh".toByteArray()

        // skip(InputStream, toSkip) 应当跳过指定字节数并返回实际跳过数量
        val input = ByteArrayInputStream(data)
        val skipped = IoKit.skip(input, 3)
        assertEquals(3, skipped)
        val remainder = input.readBytes().toString(Charsets.UTF_8)
        assertEquals("defgh", remainder)

        // skip 输入负数应抛 IllegalArgumentException
        val input2 = ByteArrayInputStream(data)
        assertFailsWith<IllegalArgumentException> {
            IoKit.skip(input2, -1)
        }

        // skipFully(InputStream, toSkip) 在数据足够时正常
        val input3 = ByteArrayInputStream(data)
        IoKit.skipFully(input3, 5)
        assertEquals("fgh", input3.readBytes().toString(Charsets.UTF_8))

        // skipFully 超出可用字节数抛 EOFException
        val input4 = ByteArrayInputStream(data)
        assertFailsWith<EOFException> {
            IoKit.skipFully(input4, 20)
        }

        // skip(Reader, toSkip) 应当跳过指定字符数并返回实际跳过数量
        val reader = StringReader("uvwxyz")
        val skippedChars = IoKit.skip(reader, 2)
        assertEquals(2, skippedChars)
        val rest = CharArray(4)
        reader.read(rest)
        assertEquals("wxyz", String(rest))

        // Reader skip 负数应抛 IllegalArgumentException
        val reader2 = StringReader("test")
        assertFailsWith<IllegalArgumentException> {
            IoKit.skip(reader2, -5)
        }

        // Reader skipFully 超出抛 EOFException
        val reader3 = StringReader("hi")
        assertFailsWith<EOFException> {
            IoKit.skipFully(reader3, 5)
        }
    }

    @Test
    fun readAndReadFullyBehaviors() {
        val text = "KOTLIN"

        // read(Reader, buffer, offset, length) 应当尽可能读取指定字符数
        val reader = StringReader(text)
        val buf = CharArray(4)
        val nRead = IoKit.read(reader, buf, 0, 4)
        assertEquals(4, nRead)
        assertEquals("KOTL", String(buf))

        // 继续读取剩余字符
        val remaining = CharArray(2)
        val nRead2 = IoKit.read(reader, remaining)
        assertEquals(2, nRead2)
        assertEquals("IN", String(remaining))

        // read 参数非法应抛 IndexOutOfBoundsException 或 IllegalArgumentException
        val reader2 = StringReader("abc")
        val buf2 = CharArray(3)
        // offset 为负，底层 StringReader.read 会抛 IndexOutOfBoundsException
        assertFailsWith<IndexOutOfBoundsException> {
            IoKit.read(reader2, buf2, -1, 2)
        }
        // length 为负，IOUtils.read 会抛 IllegalArgumentException
        assertFailsWith<IllegalArgumentException> {
            IoKit.read(reader2, buf2, 0, -2)
        }

        // readFully(Reader, buffer, offset, length) 应当严格读取指定字符数
        val reader3 = StringReader("XYZ")
        val buf3 = CharArray(3)
        val fullyRead = IoKit.readFully(reader3, buf3, 0, 3)
        assertEquals(3, fullyRead)
        assertEquals("XYZ", String(buf3))

        // readFully
        val reader4 = StringReader("AB")
        val buf4 = CharArray(3)
        IoKit.readFully(reader4, buf4, 0, 3)

        // readFully(InputStream, buffer, offset, length)
        val input = ByteArrayInputStream("1234".toByteArray())
        val byteBuf = ByteArray(4)
        IoKit.readFully(input, byteBuf, 0, 4)
        assertEquals("1234", String(byteBuf, Charsets.UTF_8))

        // readFully(InputStream, buffer) 不足时抛 IOException（部分版本会直接抛 IOException 而非 EOFException）
        val input2 = ByteArrayInputStream("12".toByteArray())
        val byteBuf2 = ByteArray(4)
        assertFailsWith<IOException> {
            IoKit.readFully(input2, byteBuf2)
        }

        // read(InputStream, buffer, offset, length) 参数非法抛 IllegalArgumentException
        val input3 = ByteArrayInputStream("data".toByteArray())
        val byteBuf3 = ByteArray(4)
        assertFailsWith<IllegalArgumentException> {
            IoKit.read(input3, byteBuf3, 0, -1)
        }

        // read(InputStream, buffer, offset, length) 正常读取
        val input4 = ByteArrayInputStream("DATA".toByteArray())
        val byteBuf4 = ByteArray(4)
        val readCount = IoKit.read(input4, byteBuf4, 0, 4)
        assertEquals(4, readCount)
        assertEquals("DATA", String(byteBuf4, Charsets.UTF_8))
    }

}
