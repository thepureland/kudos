package io.kudos.ability.file.local

import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.DownloadFileModel
import io.kudos.ability.file.local.init.LocalDownLoadService
import io.kudos.ability.file.local.init.properties.LocalProperties
import io.kudos.base.error.ServiceException
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.io.InputStreamSource
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Test cases for local file server download operations.
 *
 * Coverage:
 * - download / downloadStream happy path (including nested directories and Unicode content)
 * - non-existent file -> ServiceException(FILE_NO_EXISTS) for both byte and stream variants
 * - path traversal attempt -> ServiceException(FILE_ACCESS_DENY)
 * - missing base-path configuration -> IllegalArgumentException
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest
internal class LocalDownLoadServiceTest {

    @Resource
    private lateinit var localDownLoadService: LocalDownLoadService

    @Resource
    private lateinit var localProperties: LocalProperties

    @TempDir
    private lateinit var tempDir: Path

    @BeforeTest
    fun setTempBasePath() {
        localProperties.basePath = tempDir.toString()
    }

    private fun model(bucket: String?, path: String?): DownloadFileModel<*> =
        DownloadFileModel<InputStreamSource>().apply {
            bucketName = bucket
            filePath = path
        }

    private fun writeFile(relative: String, content: ByteArray): Path {
        val target = tempDir.resolve(relative)
        Files.createDirectories(target.parent)
        Files.write(target, content)
        return target
    }

    @Test
    fun download_returns_file_bytes() {
        val content = "hello 世界 download".toByteArray(Charsets.UTF_8)
        writeFile("bucket1/a.txt", content)

        val bytes = localDownLoadService.download(model("bucket1", "a.txt"))

        assertContentEquals(content, bytes)
    }

    @Test
    fun download_supports_nested_file_path() {
        val content = ByteArray(0) // empty file boundary
        writeFile("bucket1/sub/dir/empty.bin", content)

        val bytes = localDownLoadService.download(model("bucket1", "sub/dir/empty.bin"))

        assertEquals(0, bytes!!.size)
    }

    @Test
    fun downloadStream_returns_file_content() {
        val content = "stream-content-123".toByteArray(Charsets.UTF_8)
        writeFile("bucket2/s.txt", content)

        val stream = localDownLoadService.downloadStream(model("bucket2", "s.txt"))

        val actual = stream!!.use { it.readBytes() }
        assertContentEquals(content, actual)
    }

    @Test
    fun download_throws_when_file_not_exists() {
        val ex = assertFailsWith<ServiceException> {
            localDownLoadService.download(model("no-such-bucket", "missing.txt"))
        }
        assertEquals(FileErrorCode.FILE_NO_EXISTS, ex.errorCode)
    }

    @Test
    fun downloadStream_throws_when_file_not_exists() {
        val ex = assertFailsWith<ServiceException> {
            localDownLoadService.downloadStream(model("no-such-bucket", "missing.txt"))
        }
        assertEquals(FileErrorCode.FILE_NO_EXISTS, ex.errorCode)
    }

    @Test
    fun download_rejects_path_traversal() {
        // outside file really exists, the traversal guard must still refuse to read it
        val outside = tempDir.parent.resolve("outside-${System.nanoTime()}.txt")
        Files.write(outside, "secret".toByteArray())
        try {
            val ex = assertFailsWith<ServiceException> {
                localDownLoadService.download(model("bucket", "../../${outside.fileName}"))
            }
            assertEquals(FileErrorCode.FILE_ACCESS_DENY, ex.errorCode)
        } finally {
            Files.deleteIfExists(outside)
        }
    }

    @Test
    fun downloadStream_rejects_path_traversal_in_bucketName() {
        val ex = assertFailsWith<ServiceException> {
            localDownLoadService.downloadStream(model("..", "any.txt"))
        }
        assertEquals(FileErrorCode.FILE_ACCESS_DENY, ex.errorCode)
    }

    @Test
    fun download_fails_when_basePath_not_set() {
        localProperties.basePath = null
        val ex = assertFailsWith<IllegalArgumentException> {
            localDownLoadService.download(model("bucket", "a.txt"))
        }
        assertTrue(ex.message!!.contains("base-path is not set"))
    }

}
