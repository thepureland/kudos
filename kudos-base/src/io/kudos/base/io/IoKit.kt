package io.kudos.base.io

import org.apache.commons.io.IOUtils
import org.apache.commons.io.LineIterator
import java.io.*
import java.net.URI
import java.net.URL
import java.net.URLConnection

/**
 * IO operation utility.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object IoKit {

    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    // Wraps org.apache.commons.io.IOUtils
    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

    /**
     * Closes a URLConnection.
     *
     * @param conn the connection to close
     * @author K
     * @since 1.0.0
     */
    fun close(conn: URLConnection?): Unit = IOUtils.close(conn)

    //region toBuffered
    /**
     * Fetches the entire contents of an `InputStream` and returns the same data as a result InputStream.
     * This method is useful in cases where:
     *  the source InputStream is slow.
     *  it has associated network resources, so we can't keep it open for too long.
     *  it has associated network timeouts.
     *
     * It can be used as input for [.toByteArray], because it avoids unnecessary allocation and copy of byte[].
     *
     * This method buffers the input internally, so there is no need to use a `BufferedInputStream`.
     *
     * @param input the Stream to be fully buffered
     * @return a fully buffered Stream
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun toBufferedInputStream(input: InputStream): InputStream? = IOUtils.toBufferedInputStream(input)

    /**
     * Returns the given reader if it is a [BufferedReader]; otherwise creates a BufferedReader
     * for the given reader and returns it.
     *
     * @param reader the reader to wrap or return
     * @return the given Reader or a new [BufferedReader] for the given Reader
     * @author K
     * @since 1.0.0
     */
    fun toBufferedReader(reader: Reader?): BufferedReader? = IOUtils.toBufferedReader(reader)
    //endregion toBuffered

    //region read toByteArray
    // -----------------------------------------------------------------------
    /**
     * Reads the contents of an `InputStream` as a `byte[]`.
     *
     * This method buffers the input internally, so there is no need to use a `BufferedInputStream`.
     *
     * @param input the `InputStream` to read
     * @return the requested byte array
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun toByteArray(input: InputStream): ByteArray = IOUtils.toByteArray(input)

    /**
     * Reads the contents of an `InputStream` as a `byte[]`.
     * Use this method instead of `toByteArray(InputStream)` when the size of the `InputStream` is known.
     *
     * **Note:** this method checks that the length can be safely cast to int (because array length
     * cannot exceed Integer.MAX_VALUE) before allocating via [IoKit.toByteArray].
     *
     * @param input the `InputStream` to read
     * @param size the size of the `InputStream`
     * @return the requested byte array
     * @throws IOException when an I/O error occurs or the size of `InputStream` differs from the size parameter
     * @throws IllegalArgumentException if the size parameter is negative or greater than Integer.MAX_VALUE
     * @see IoKit.toByteArray
     * @author K
     * @since 1.0.0
     */
    fun toByteArray(input: InputStream, size: Long): ByteArray = IOUtils.toByteArray(input, size)

    /**
     * Reads the contents of an `InputStream` as a `byte[]`.
     * Use this method instead of `toByteArray(InputStream)` when the size of the `InputStream` is known.
     *
     * @param input the `InputStream` to read
     * @param size the size of the `InputStream`
     * @return the requested byte array
     * @throws IOException when an I/O error occurs or the size of `InputStream` differs from the size parameter
     * @throws IllegalArgumentException if the size parameter is negative
     * @author K
     * @since 1.0.0
     */
    fun toByteArray(input: InputStream, size: Int): ByteArray = IOUtils.toByteArray(input, size)

    /**
     * Reads the contents of a `Reader` as a `byte[]` using the specified character encoding.
     * Character encoding names can be found at [IANA](http://www.iana.org/assignments/character-sets).
     * This method buffers the input internally, so there is no need to use a `BufferedReader`.
     *
     * @param input the `Reader` to read
     * @param encoding the encoding
     * @return the requested byte array
     * @throws IOException when an I/O error occurs
     * @throws java.nio.charset.UnsupportedCharsetException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun toByteArray(input: Reader, encoding: String? = null): ByteArray = IOUtils.toByteArray(input, encoding)

    /**
     * Reads the contents pointed to by the `URI` as a `byte[]`.
     *
     * @param uri the `URI` whose content to read
     * @return the requested byte array
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun toByteArray(uri: URI): ByteArray = IOUtils.toByteArray(uri)

    /**
     * Reads the contents pointed to by the `URL` as a `byte[]`.
     *
     * @param url the `URL` whose content to read
     * @return the requested byte array
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun toByteArray(url: URL): ByteArray = IOUtils.toByteArray(url)

    /**
     * Reads the contents pointed to by the `URLConnection` as a `byte[]`.
     *
     * @param urlConn the `URLConnection` whose content to read
     * @return the requested byte array
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun toByteArray(urlConn: URLConnection): ByteArray = IOUtils.toByteArray(urlConn)
    //endregion read toByteArray

    //region read char[]
    /**
     * Reads the contents of an `InputStream` as a `char[]`, using the specified character encoding.
     *
     * Character encoding names can be found at
     * [IANA](http://www.iana.org/assignments/character-sets).
     *
     * This method buffers the input internally, so there is no need to use a `BufferedInputStream`.
     *
     * @param is the `InputStream` to read
     * @param encoding the encoding to use; null means the platform default encoding
     * @return the requested character array
     * @throws IOException when an I/O error occurs
     * @throws java.nio.charset.UnsupportedCharsetException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun toCharArray(`is`: InputStream, encoding: String? = null): CharArray = IOUtils.toCharArray(`is`, encoding)

    /**
     * Reads the contents of a `Reader` as a `char[]`.
     * This method buffers the input internally, so there is no need to use a `BufferedReader`.
     *
     * @param input the `Reader` to read
     * @return the requested character array
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun toCharArray(input: Reader): CharArray = IOUtils.toCharArray(input)
    //endregion read char[]

    //region read toString
    /**
     * Reads the contents of an `InputStream` as a String, using the specified character encoding.
     * Character encoding names can be found at
     * [IANA](http://www.iana.org/assignments/character-sets).
     *
     * This method buffers the input internally, so there is no need to use a `BufferedInputStream`.
     *
     * @param input the `InputStream` to read
     * @param encoding the encoding to use; null means the platform default encoding
     * @return the requested String
     * @throws IOException when an I/O error occurs
     * @throws java.nio.charset.UnsupportedCharsetException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun toString(input: InputStream, encoding: String? = null): String = IOUtils.toString(input, encoding)

    /**
     * Reads the contents of a `Reader` as a String.
     * This method buffers the input internally, so there is no need to use a `BufferedReader`.
     *
     * @param input the `Reader` to read
     * @return the requested String
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun toString(input: Reader): String = IOUtils.toString(input)

    /**
     * Reads the contents pointed to by the `URI` as a String.
     *
     * @param uri the URI source
     * @param encoding the encoding name of the content pointed to by the URL
     * @return the String representation of the content pointed to by the URI
     * @throws IOException when an I/O error occurs
     * @throws java.nio.charset.UnsupportedCharsetException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun toString(uri: URI, encoding: String? = null): String = IOUtils.toString(uri, encoding)

    /**
     * Reads the contents pointed to by the `URL` as a String.
     *
     * @param url the URL source
     * @param encoding the encoding name of the content pointed to by the URL
     * @return the String representation of the content pointed to by the URI
     * @if an I/O exception occurs.
     * @throws IOException when an I/O error occurs
     * @throws java.nio.charset.UnsupportedCharsetException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun toString(url: URL, encoding: String? = null): String = IOUtils.toString(url, encoding)

    /**
     * Reads the contents of a `byte[]` as a String, using the specified character encoding.
     *
     * Character encoding names can be found at [IANA](http://www.iana.org/assignments/character-sets).
     *
     * @param input the byte array to read
     * @param encoding the encoding to use; null means the platform default encoding
     * @return the requested String
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun toString(input: ByteArray, encoding: String? = null): String = IOUtils.toString(input, encoding)
    //endregion read toString

    //region readLines
    /**
     * Reads the contents of an `InputStream` as a list of Strings, one entry per line,
     * using the specified character encoding.
     * Character encoding names can be found at [IANA](http://www.iana.org/assignments/character-sets).
     * This method buffers the input internally, so there is no need to use a `BufferedInputStream`.
     *
     * @param input the `InputStream` to read, not null
     * @param encoding the encoding to use; null means the platform default encoding
     * @return the list of Strings, never null
     * @throws IOException when an I/O error occurs
     * @throws java.nio.charset.UnsupportedCharsetException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun readLines(input: InputStream, encoding: String? = null): List<String> = IOUtils.readLines(input, encoding)

    /**
     * Reads the contents of a `Reader` as a list of Strings, one entry per line.
     * This method buffers the input internally, so there is no need to use a `BufferedReader`.
     *
     * @param input the `Reader` to read, not null
     * @return the list of Strings, never null
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun readLines(input: Reader): List<String> = IOUtils.readLines(input)
    //endregion readLines

    //region lineIterator
    /**
     * Returns an Iterator for the lines in a `Reader`.
     *
     * `LineIterator` holds a reference to the open `Reader`.
     * When you have finished iterating, you should close the `Reader` to free internal resources.
     * This can be done by closing the `Reader` directly, by calling [LineIterator.close],
     * or by calling [LineIterator.closeQuietly].
     *
     * Recommended usage pattern:
     *
     * <pre>
     * try {
     * LineIterator it = IOUtils.lineIterator(reader);
     * while (it.hasNext()) {
     * String line = it.nextLine();
     * // / do something with line
     * }
     * } finally {
     * IOUtils.closeQuietly(reader);
     * }
     * </pre>
     * @param reader the `Reader` to read
     * @return an iterator of the lines
     * @author K
     * @since 1.0.0
     */
    fun lineIterator(reader: Reader): LineIterator = IOUtils.lineIterator(reader)

    /**
     * Returns an Iterator for the lines in an `InputStream`, using the specified encoding
     * (null means platform default encoding).
     *
     * `LineIterator` holds a reference to the open `InputStream`.
     * When you have finished iterating, you should close the `InputStream` to free internal resources.
     * This can be done by closing the `InputStream` directly, by calling [LineIterator.close],
     * or by calling [LineIterator.closeQuietly].
     *
     * Recommended usage pattern:
     *
     * <pre>
     * try {
     * LineIterator it = IOUtils.lineIterator(stream, &quot;UTF-8&quot;);
     * while (it.hasNext()) {
     * String line = it.nextLine();
     * // / do something with line
     * }
     * } finally {
     * IOUtils.closeQuietly(stream);
     * }
     * </pre>
     * @param input the `InputStream` to read, not null
     * @param encoding the encoding to use; null means the platform default encoding
     * @return an iterator of the lines
     * @throws IOException when an I/O error occurs
     * @throws java.nio.charset.UnsupportedCharsetException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun lineIterator(input: InputStream, encoding: String? = null): LineIterator = IOUtils.lineIterator(input, encoding)
    //endregion lineIterator

    //region toInputStream
    /**
     * Converts the specified CharSequence to an InputStream using the specified character encoding.
     * Character encoding names can be found at [IANA](http://www.iana.org/assignments/character-sets).
     *
     * @param input the CharSequence to convert
     * @param encoding the encoding to use; null means the platform default encoding
     * @return an input stream
     * @throws IOException when an I/O error occurs
     * @throws java.nio.charset.UnsupportedCharsetException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun toInputStream(input: CharSequence, encoding: String? = null): InputStream =
        IOUtils.toInputStream(input, encoding)

    /**
     * Converts the specified String to an InputStream using the specified character encoding.
     * Character encoding names can be found at [IANA](http://www.iana.org/assignments/character-sets).
     *
     * @param input the String to convert
     * @param encoding the encoding to use; null means the platform default encoding
     * @return an input stream
     * @throws IOException when an I/O error occurs
     * @throws java.nio.charset.UnsupportedCharsetException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun toInputStream(input: String, encoding: String? = null): InputStream = IOUtils.toInputStream(input, encoding)
    //endregion toInputStream

    //region write byte[]
    /**
     * Writes the contents of a `byte[]` to an `OutputStream`.
     *
     * @param data the byte array to write, not modified during output
     * @param output the `OutputStream` to write to
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun write(data: ByteArray, output: OutputStream): Unit = IOUtils.write(data, output)

    /**
     * Writes the contents of a `byte[]` to a `Writer`, using the specified default character encoding.
     * Character encoding names can be found at [IANA](http://www.iana.org/assignments/character-sets).
     *
     * @param data the byte array to write, not modified during output
     * @param output the `Writer` to write to
     * @param encoding the encoding to use; null means the platform default encoding
     * @throws IOException when an I/O error occurs
     * @throws java.nio.charset.UnsupportedCharsetException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun write(data: ByteArray, output: Writer, encoding: String? = null): Unit = IOUtils.write(data, output, encoding)
    //endregion write byte[]

    //region write char[]
    /**
     * Writes the contents of a `char[]` to a `Writer`, using the platform default character encoding.
     * @param data the character array to write, not modified during output
     * @param output the `Writer` to write to
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun write(data: CharArray, output: Writer): Unit = IOUtils.write(data, output)

    /**
     * Writes the contents of a `char[]` to an `OutputStream`, using the specified character encoding.
     * Character encoding names can be found at [IANA](http://www.iana.org/assignments/character-sets).
     *
     * @param data the character array to write, not modified during output
     * @param output the `OutputStream` to write to
     * @param encoding the encoding to use; null means the platform default encoding
     * @throws IOException when an I/O error occurs
     * @throws java.nio.charset.UnsupportedCharsetException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun write(data: CharArray, output: OutputStream, encoding: String? = null): Unit =
        IOUtils.write(data, output, encoding)

    //endregion write char[]

    //region write CharSequence
    /**
     * Writes the contents of a `CharSequence` to a `Writer`.
     *
     * @param data the `CharSequence` to write
     * @param output the `Writer` to write to
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun write(data: CharSequence, output: Writer): Unit = IOUtils.write(data, output)

    /**
     * Writes the contents of a `CharSequence` to an `OutputStream`, using the specified character encoding.
     * Character encoding names can be found at [IANA](http://www.iana.org/assignments/character-sets).
     *
     *
     * @param data the `CharSequence` to write
     * @param output the `OutputStream` to write to
     * @param encoding the encoding to use; null means the platform default encoding
     * @throws IOException when an I/O error occurs
     * @throws java.nio.charset.UnsupportedCharsetException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun write(data: CharSequence, output: OutputStream, encoding: String? = null): Unit =
        IOUtils.write(data, output, encoding)
    //endregion write CharSequence

    //region write String
    /**
     * Writes the contents of a `String` to a `Writer`.
     *
     * @param data the `String` to write
     * @param output the `Writer` to write to
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun write(data: String, output: Writer): Unit = IOUtils.write(data, output)

    /**
     * Writes the contents of a `CharSequence` to an `OutputStream`, using the specified character encoding.
     * Character encoding names can be found at [IANA](http://www.iana.org/assignments/character-sets).
     *
     * @param data the `String` to write
     * @param output the `OutputStream` to write to
     * @param encoding the encoding to use; null means the platform default encoding
     * @throws IOException when an I/O error occurs
     * @throws java.nio.charset.UnsupportedCharsetException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun write(data: String, output: OutputStream, encoding: String? = null): Unit =
        IOUtils.write(data, output, encoding)
    //endregion write String

    //region writeLines
    /**
     * Writes the toString() result of each element in a collection line by line to an `OutputStream`,
     * using the specified line separator and character encoding.
     *
     * Character encoding names can be found at [IANA](http://www.iana.org/assignments/character-sets).
     *
     * @param lines the lines to write; null entries produce blank lines
     * @param lineEnding the line separator to use; null uses the system default line separator
     * @param output the `OutputStream` to write to; not null and not closed
     * @param encoding the encoding to use; null means the platform default encoding
     * @throws IOException when an I/O error occurs
     * @throws java.nio.charset.UnsupportedCharsetException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun writeLines(
        lines: Collection<*>, lineEnding: String? = null, output: OutputStream, encoding: String? = null
    ): Unit = IOUtils.writeLines(lines, lineEnding, output, encoding)

    /**
     * Writes the toString() result of each element in a collection line by line to a `Writer`.
     *
     * @param lines the lines to write; null entries produce blank lines
     * @param lineEnding the line separator to use; null uses the system default line separator
     * @param writer the `Writer` to write to; not null and not closed
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun writeLines(lines: Collection<*>, lineEnding: String? = null, writer: Writer): Unit =
        IOUtils.writeLines(lines, lineEnding, writer)
    //endregion writeLines

    //region copy from InputStream
    /**
     * Copies bytes from an `InputStream` to an `OutputStream`.
     * This method buffers the input internally, so there is no need to use a `BufferedInputStream`.
     *
     * Large streams (over 2GB) will return a byte copy count of `-1` after the copy completes,
     * because the correct number of bytes cannot be returned as an int. For large streams,
     * use the `copyLarge(InputStream, OutputStream)` method.
     *
     * @param input the `InputStream` to read
     * @param output the `OutputStream` to write to
     * @return the number of bytes copied; -1 if greater than Integer.MAX_VALUE
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun copy(input: InputStream, output: OutputStream): Int = IOUtils.copy(input, output)

    /**
     * Copies bytes from a large (over 2GB) `InputStream` to an `OutputStream`.
     * This method buffers the input internally, so there is no need to use a `BufferedInputStream`.
     *
     * @param input the `InputStream` to read
     * @param output the `OutputStream` to write to
     * @return the number of bytes copied
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun copyLarge(input: InputStream, output: OutputStream): Long = IOUtils.copyLarge(input, output)

    /**
     * Copies bytes from a large (over 2GB) `InputStream` to an `OutputStream`.
     * This method uses the provided buffer, so there is no need to use a `BufferedInputStream`.
     *
     * @param input the `InputStream` to read
     * @param output the `OutputStream` to write to
     * @param buffer the buffer to use for the copy
     * @return the number of bytes copied
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun copyLarge(input: InputStream, output: OutputStream, buffer: ByteArray): Long =
        IOUtils.copyLarge(input, output, buffer)

    /**
     * Copies all or part of the bytes from a large (over 2GB) `InputStream` to an `OutputStream`,
     * optionally skipping some input bytes.
     * This method buffers the input internally, so there is no need to use a `BufferedInputStream`.
     *
     * @param input the `InputStream` to read
     * @param output the `OutputStream` to write to
     * @param inputOffset the number of bytes to skip from the input before copying; negative copies all
     * @param length the number of bytes to copy; negative copies all
     * @return the number of bytes copied
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun copyLarge(input: InputStream, output: OutputStream, inputOffset: Long, length: Long): Long =
        IOUtils.copyLarge(input, output, inputOffset, length)

    /**
     * Copies all or part of the bytes from a large (over 2GB) `InputStream` to an `OutputStream`,
     * optionally skipping some input bytes.
     * This method uses the provided buffer, so there is no need to use a `BufferedInputStream`.
     *
     * @param input the `InputStream` to read
     * @param output the `OutputStream` to write to
     * @param inputOffset the number of bytes to skip from the input before copying; negative copies all
     * @param length the number of bytes to copy; negative copies all
     * @param buffer the buffer to use for the copy
     * @return the number of bytes copied
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun copyLarge(input: InputStream, output: OutputStream, inputOffset: Long, length: Long, buffer: ByteArray): Long =
        IOUtils.copyLarge(input, output, inputOffset, length, buffer)

    /**
     * Copies the contents of an `InputStream` to a `Writer`, using the specified character encoding.
     * This method buffers the input internally, so there is no need to use a `BufferedInputStream`.
     * Character encoding names can be found at [IANA](http://www.iana.org/assignments/character-sets).
     * This method uses [InputStreamReader].
     *
     * @param input the `InputStream` to read
     * @param output the `Writer` to write to
     * @param encoding the encoding to use; null means the platform default encoding
     * @throws IOException when an I/O error occurs
     * @throws java.nio.charset.UnsupportedCharsetException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun copy(input: InputStream, output: Writer, encoding: String? = null): Unit =
        IOUtils.copy(input, output, encoding)
    //endregion copy from InputStream

    //region copy from Reader
    /**
     * Copies characters from a `Reader` to a `Writer`.
     * This method buffers the input internally, so there is no need to use a `BufferedReader`.
     *
     * Large streams (over 2GB) will return a byte copy count of `-1` after the copy completes,
     * because the correct number of bytes cannot be returned as an int. For large streams,
     * use the `copyLarge(Reader, Writer)` method.
     *
     * @param input the `Reader` to read
     * @param output the `Writer` to write to
     * @return the number of characters copied; -1 if greater than Integer.MAX_VALUE
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun copy(input: Reader, output: Writer): Int = IOUtils.copy(input, output)

    /**
     * Copies characters from a large (over 2GB) `Reader` to a `Writer`.
     * This method buffers the input internally, so there is no need to use a `BufferedReader`.
     *
     * @param input the `Reader` to read
     * @param output the `Writer` to write to
     * @return the number of characters copied
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun copyLarge(input: Reader, output: Writer): Long = IOUtils.copyLarge(input, output)

    /**
     * Copies characters from a large (over 2GB) `Reader` to a `Writer`.
     * This method uses the provided buffer, so there is no need to use a `BufferedReader`.
     *
     * @param input the `Reader` to read
     * @param output the `Writer` to write to
     * @param buffer the buffer to use for the copy
     * @return the number of characters copied
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun copyLarge(input: Reader, output: Writer, buffer: CharArray): Long = IOUtils.copyLarge(input, output, buffer)

    /**
     * Copies all or part of the characters from a large (over 2GB) `Reader` to a `Writer`,
     * optionally skipping some characters.
     * This method buffers the input internally, so there is no need to use a `BufferedReader`.
     *
     * @param input the `Reader` to read
     * @param output the `Writer` to write to
     * @param inputOffset the number of characters to skip from the input before copying; negative copies all
     * @param length the number of characters to copy; negative copies all
     * @return the number of characters copied
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun copyLarge(input: Reader, output: Writer, inputOffset: Long, length: Long): Long =
        IOUtils.copyLarge(input, output, inputOffset, length)

    /**
     * Copies all or part of the characters from a large (over 2GB) `Reader` to a `Writer`,
     * optionally skipping some characters.
     *
     * This method uses the provided buffer, so there is no need to use a `BufferedReader`.
     * @param input the `Reader` to read
     * @param output the `Writer` to write to
     * @param inputOffset the number of characters to skip from the input before copying; negative copies all
     * @param length the number of characters to copy; negative copies all
     * @param buffer the buffer to use for the copy
     * @return the number of characters copied
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun copyLarge(input: Reader, output: Writer, inputOffset: Long, length: Long, buffer: CharArray): Long =
        IOUtils.copyLarge(input, output, inputOffset, length, buffer)

    /**
     * Copies the contents of a `Reader` to an `OutputStream`,
     * using the specified character encoding, and calls flush.
     * This method buffers the input internally, so there is no need to use a `BufferedReader`.
     * Character encoding names can be found at [IANA](http://www.iana.org/assignments/character-sets).
     * Because of the implementation of OutputStreamWriter, this method can perform a flush.
     * This method uses [OutputStreamWriter].
     *
     * @param input the `Reader` to read
     * @param output the `OutputStream` to write to
     * @param encoding the encoding to use; null means the platform default encoding
     * @throws IOException when an I/O error occurs
     * @throws java.nio.charset.UnsupportedCharsetException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun copy(input: Reader, output: OutputStream, encoding: String? = null): Unit =
        IOUtils.copy(input, output, encoding)
    //endregion copy from Reader

    //region content equals
    /**
     * Checks whether the contents of two input streams are equal.
     * If the inputs are not buffered, this method internally uses `BufferedInputStream` to buffer the inputs.
     *
     * @param input1 the first input stream
     * @param input2 the second input stream
     * @return true if the contents of both input streams are equal, or if they both do not exist; otherwise false
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun contentEquals(input1: InputStream, input2: InputStream): Boolean = IOUtils.contentEquals(input1, input2)

    /**
     * Checks whether the contents of two Readers are equal.
     * If the inputs are not buffered, this method internally uses `BufferedReader` to buffer the inputs.
     *
     * @param input1 the first reader
     * @param input2 the second reader
     * @return true if the contents of both readers are equal, or if they both do not exist; otherwise false
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun contentEquals(input1: Reader, input2: Reader): Boolean = IOUtils.contentEquals(input1, input2)

    /**
     * Checks whether the contents of two Readers are equal, ignoring EOL characters.
     * If the inputs are not buffered, this method internally uses `BufferedReader` to buffer the inputs.
     *
     * @param input1 the first reader
     * @param input2 the second reader
     * @return true if the contents of both readers are equal (ignoring EOL characters), or if they both do not exist; otherwise false
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun contentEqualsIgnoreEOL(input1: Reader, input2: Reader): Boolean = IOUtils.contentEqualsIgnoreEOL(input1, input2)
    //endregion content equals

    //region skip
    /**
     * Skips bytes from a byte stream. This implementation guarantees that it will read as many bytes
     * as possible before giving up, unlike its subclass [Reader].
     *
     * @param input the byte stream to skip
     * @param toSkip the number of bytes to skip
     * @return the actual number of bytes skipped
     * @throws IllegalArgumentException if the toSkip parameter is negative
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun skip(input: InputStream, toSkip: Long): Long = IOUtils.skip(input, toSkip)

    /**
     * Skips characters from a character stream. This implementation guarantees that it will read as many characters
     * as possible before giving up, unlike its subclass [Reader].
     *
     * @param input the byte stream to skip
     * @param toSkip the number of bytes to skip
     * @return the actual number of bytes skipped
     * @see Reader.skip
     * @throws IllegalArgumentException if the toSkip parameter is negative
     * @throws IOException when an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun skip(input: Reader, toSkip: Long): Long = IOUtils.skip(input, toSkip)

    /**
     * Skips the requested number of bytes, or fails if there are not enough bytes.
     * This method allows [InputStream.skip] not to skip as many bytes as specified by the argument
     * (most likely because of reaching the end of the file).
     *
     * @param input the stream to skip
     * @param toSkip the number of bytes to skip; must not be negative
     * @see InputStream.skip
     * @throws IOException if an error occurs while reading
     * @throws IllegalArgumentException if the specified number of bytes is negative
     * @throws EOFException if the number of bytes to skip is incorrect
     * @author K
     * @since 1.0.0
     */
    fun skipFully(input: InputStream, toSkip: Long): Unit = IOUtils.skipFully(input, toSkip)

    /**
     * Skips the requested number of characters, or fails if there are not enough characters.
     * This method allows [Reader.skip] not to skip as many characters as specified by the argument
     * (most likely because of reaching the end of the file).
     *
     * @param input the stream to skip
     * @param toSkip the number of characters to skip; must not be negative
     * @see Reader.skip
     * @throws IOException if an error occurs while reading
     * IllegalArgumentException if the specified number of characters is negative
     * EOFException if the number of characters to skip is incorrect
     * @author K
     * @since 1.0.0
     */
    fun skipFully(input: Reader?, toSkip: Long): Unit = IOUtils.skipFully(input, toSkip)
    //endregion skip

    //region read
    /**
     * Reads characters from a character stream. This implementation guarantees that it will read as many characters
     * as possible before giving up, unlike its subclass [Reader].
     *
     * @param input the character stream to read into characters
     * @param buffer the destination
     * @param offset the initial offset into the buffer
     * @param length the length to read; must be >= 0
     * @return the actual length read; may be smaller than requested if the end of file is reached
     * @throws IOException if an error occurs while reading
     * @author K
     * @since 1.0.0
     */
    fun read(input: Reader, buffer: CharArray, offset: Int, length: Int): Int {
        require(length >= 0) { "Length must not be negative: $length" }
        return IOUtils.read(input, buffer, offset, length)
    }

    /**
     * Reads characters from a character stream. This implementation guarantees that it will read as many characters
     * as possible before giving up, unlike its subclass [Reader].
     *
     * @param input the character stream to read into characters
     * @param buffer the destination
     * @return the actual length read; may be smaller than requested if the end of file is reached
     * @throws IOException if an error occurs while reading
     * @author K
     * @since 1.0.0
     */
    fun read(input: Reader, buffer: CharArray): Int = IOUtils.read(input, buffer)

    /**
     * Reads bytes from a byte stream. This implementation guarantees that it will read as many bytes
     * as possible before giving up, unlike its subclass [InputStream].
     *
     * @param input the byte stream to read into characters
     * @param buffer the destination
     * @param offset the initial offset into the buffer
     * @param length the length to read; must be >= 0
     * @return the actual length read; may be smaller than requested if the end of file is reached
     * @throws IOException if an error occurs while reading
     * @author K
     * @since 1.0.0
     */
    fun read(input: InputStream, buffer: ByteArray, offset: Int, length: Int): Int {
        require(length >= 0) { "Length must not be negative: $length" }
        return IOUtils.read(input, buffer, offset, length)
    }

    /**
     * Reads characters from a byte stream. This implementation guarantees that it will read as many bytes
     * as possible before giving up, unlike its subclass [InputStream].
     *
     * @param input the byte stream to read into characters
     * @param buffer the destination
     * @return the actual length read; may be smaller than requested if the end of file is reached
     * @throws IOException if an error occurs while reading
     * @author K
     * @since 1.0.0
     */
    fun read(input: InputStream, buffer: ByteArray): Int = IOUtils.read(input, buffer)

    /**
     * Reads the requested number of characters, or fails if there are not enough characters.
     *
     * This method allows [Reader.read] not to skip as many characters as specified by the argument
     * (most likely because of reaching the end of the file).
     *
     * @param input the byte stream to read into characters
     * @param buffer the destination
     * @param offset the initial offset into the buffer
     * @param length the length to read; must be >= 0
     * @return the actual length read
     * @throws IOException if an error occurs while reading
     * @throws IllegalArgumentException if the specified number of characters is negative
     * @throws EOFException if the number of characters to skip is incorrect
     * @author K
     * @since 1.0.0
     */
    fun readFully(input: Reader, buffer: CharArray, offset: Int, length: Int): Int {
        require(length >= 0) { "Length must not be negative: $length" }
        return IOUtils.read(input, buffer, offset, length)
    }

    /**
     * Reads the requested number of characters, or fails if there are not enough characters.
     *
     * This method allows [Reader.read] not to skip as many characters as specified by the argument
     * (most likely because of reaching the end of the file).
     *
     * @param input the byte stream to read into characters
     * @param buffer the destination
     * @throws IOException if an error occurs while reading
     * @throws IllegalArgumentException if the specified number of characters is negative
     * @throws EOFException if the number of characters to skip is incorrect
     * @author K
     * @since 1.0.0
     */
    fun readFully(input: Reader, buffer: CharArray): Unit = IOUtils.readFully(input, buffer)

    /**
     * Reads the requested number of bytes, or fails if there are not enough bytes.
     * This method allows [InputStream.read] not to skip as many bytes as specified by the argument
     * (most likely because of reaching the end of the file).
     *
     * @param input the byte stream to read into bytes
     * @param buffer the destination
     * @param offset the initial offset into the buffer
     * @param length the length to read; must be >= 0
     * @throws IOException if an error occurs while reading
     * @throws IllegalArgumentException if the specified number of bytes is negative
     * @throws EOFException if the number of bytes to skip is incorrect
     * @author K
     * @since 1.0.0
     */
    fun readFully(input: InputStream, buffer: ByteArray, offset: Int, length: Int) {
        require(length >= 0) { "Length must not be negative: $length" }
        IOUtils.readFully(input, buffer, offset, length)
    }

    /**
     * Reads the requested number of bytes, or fails if there are not enough bytes.
     * This method allows [InputStream.read] not to skip as many bytes as specified by the argument
     * (most likely because of reaching the end of the file).
     *
     * @param input the byte stream to read into bytes
     * @param buffer the destination
     * @throws IOException if an error occurs while reading
     * @throws IllegalArgumentException if the specified number of bytes is negative
     * @throws EOFException if the number of bytes to skip is incorrect
     * @author K
     * @since 1.0.0
     */
    fun readFully(input: InputStream, buffer: ByteArray): Unit = IOUtils.readFully(input, buffer)
    //endregion read

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // Wraps org.apache.commons.io.IOUtils
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

}
