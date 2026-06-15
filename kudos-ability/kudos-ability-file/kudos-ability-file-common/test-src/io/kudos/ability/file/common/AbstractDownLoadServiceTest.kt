package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.DownloadFileModel
import org.springframework.core.io.InputStreamSource
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Tests for [AbstractDownLoadService].
 *
 * Covers:
 *  - download/downloadStream delegate to subclass implementations and return their results (including null)
 *  - exceptions from subclass implementations are logged then rethrown unchanged (same instance)
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AbstractDownLoadServiceTest {

    private class FakeDownLoadService(
        private val bytes: ByteArray?,
        private val stream: InputStream?,
        private val error: Exception? = null
    ) : AbstractDownLoadService() {

        override fun readFileToByte(downloadFileModel: DownloadFileModel<*>): ByteArray? {
            error?.let { throw it }
            return bytes
        }

        override fun readFileToStream(downloadFileModel: DownloadFileModel<*>): InputStream? {
            error?.let { throw it }
            return stream
        }
    }

    private val model = DownloadFileModel<InputStreamSource>().apply {
        bucketName = "bucket"
        filePath = "/dir/a.txt"
    }

    @Test
    fun download_returnsBytesFromImplementation() {
        val data = byteArrayOf(1, 2, 3)
        val service = FakeDownLoadService(data, null)
        assertContentEquals(data, service.download(model))
    }

    @Test
    fun download_returnsNullWhenImplementationReturnsNull() {
        val service = FakeDownLoadService(null, null)
        assertNull(service.download(model))
    }

    @Test
    fun download_logsAndRethrowsSameException() {
        val boom = IOException("file storage down")
        val service = FakeDownLoadService(null, null, boom)
        val thrown = assertFailsWith<IOException> { service.download(model) }
        assertSame(boom, thrown)
        assertEquals("file storage down", thrown.message)
    }

    @Test
    fun downloadStream_returnsStreamFromImplementation() {
        val stream = ByteArrayInputStream(byteArrayOf(9, 8))
        val service = FakeDownLoadService(null, stream)
        assertSame(stream, service.downloadStream(model))
    }

    @Test
    fun downloadStream_returnsNullWhenImplementationReturnsNull() {
        val service = FakeDownLoadService(null, null)
        assertNull(service.downloadStream(model))
    }

    @Test
    fun downloadStream_logsAndRethrowsSameException() {
        val boom = IllegalStateException("not connected")
        val service = FakeDownLoadService(null, null, boom)
        val thrown = assertFailsWith<IllegalStateException> { service.downloadStream(model) }
        assertSame(boom, thrown)
    }

}
