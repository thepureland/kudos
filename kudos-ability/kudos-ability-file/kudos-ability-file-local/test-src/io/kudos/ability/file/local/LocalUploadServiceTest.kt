package io.kudos.ability.file.local

import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.local.init.LocalUploadService
import io.kudos.ability.file.local.init.properties.LocalProperties
import io.kudos.base.error.ServiceException
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.io.InputStreamResource
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test cases for local file server upload operations.
 *
 * @author unknown
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest
internal class LocalUploadServiceTest {

    @Resource
    private lateinit var localUploadService: LocalUploadService

    @Resource
    private lateinit var localProperties: LocalProperties

    @TempDir
    private lateinit var tempDir: Path

    @BeforeTest
    fun setTempBasePath() {
        localProperties.basePath = tempDir.toString()
    }

    @Test
    fun fileUpload_with_category() {
        val resourceAsStream =
            LocalUploadServiceTest::class.java.getClassLoader().getResourceAsStream("files/test-file.txt")
        val uploadFileModel = UploadFileModel<InputStreamResource>()
        uploadFileModel.fileSuffix = "txt"
        uploadFileModel.bucketName = "test"
        uploadFileModel.tenantId = "0"
        uploadFileModel.category = "doc"
        uploadFileModel.inputStreamSource = InputStreamResource(resourceAsStream)

        val uploadFileResult = localUploadService.fileUpload(uploadFileModel)
        val filename = uploadFileResult.filePath

        val pathSnippet = listOf("0", "doc").joinToString("/")
        val filePath = checkNotNull(filename) { "filePath must not be null" }
        assertTrue(filePath.contains(pathSnippet))
        assertTrue(Files.exists(tempDir.resolve(filePath.removePrefix("/"))))
    }

    @Test
    fun fileUpload_without_category() {
        val resourceAsStream =
            LocalUploadServiceTest::class.java.getClassLoader().getResourceAsStream("files/test-file.txt")
        val uploadFileModel = UploadFileModel<InputStreamResource>()
        uploadFileModel.fileSuffix = "txt"
        uploadFileModel.bucketName = "test"
        uploadFileModel.tenantId = "0"
        uploadFileModel.inputStreamSource = InputStreamResource(resourceAsStream)
        val uploadFileResult = localUploadService.fileUpload(uploadFileModel)
        val filename = uploadFileResult.filePath

        val today = LocalDate.now()
        val pathSnippet = listOf("0", today.year.toString(), today.monthValue.toString(), today.dayOfMonth.toString())
            .joinToString("/")
        val filePath = checkNotNull(filename) { "filePath must not be null" }
        assertTrue(filePath.contains(pathSnippet))
        assertTrue(Files.exists(tempDir.resolve(filePath.removePrefix("/"))))
    }

    @Test
    fun fileUpload_rejects_pathTraversalFileName() {
        val uploadFileModel = UploadFileModel<InputStreamResource>()
        uploadFileModel.fileSuffix = "txt"
        uploadFileModel.bucketName = "test"
        uploadFileModel.tenantId = "0"
        uploadFileModel.fileName = "../escape.txt"
        uploadFileModel.inputStreamSource = InputStreamResource(ByteArrayInputStream("x".toByteArray()))

        assertFailsWith<ServiceException> {
            localUploadService.fileUpload(uploadFileModel)
        }
        assertFalse(Files.exists(tempDir.parent.resolve("escape.txt")))
    }

    @Test
    fun fileUpload_without_bucketName_and_with_explicit_fileName() {
        val content = "explicit name 内容".toByteArray(Charsets.UTF_8)
        val uploadFileModel = UploadFileModel<InputStreamResource>()
        uploadFileModel.fileSuffix = "txt"
        uploadFileModel.bucketName = null
        uploadFileModel.tenantId = "0"
        uploadFileModel.category = "doc"
        uploadFileModel.fileName = "测试-explicit.txt"
        uploadFileModel.inputStreamSource = InputStreamResource(ByteArrayInputStream(content))

        val result = localUploadService.fileUpload(uploadFileModel)

        val filePath = checkNotNull(result.filePath)
        assertTrue(filePath.startsWith("/0/doc/"))
        assertTrue(filePath.endsWith("/测试-explicit.txt"))
        assertEquals("", result.pathPrefix)
        val saved = tempDir.resolve(filePath.removePrefix("/"))
        assertTrue(Files.exists(saved))
        assertTrue(content.contentEquals(Files.readAllBytes(saved)))
    }

    @Test
    fun fileUpload_with_blank_bucketName_falls_back_to_fileDir_only() {
        val uploadFileModel = UploadFileModel<InputStreamResource>()
        uploadFileModel.fileSuffix = "txt"
        uploadFileModel.bucketName = "  "
        uploadFileModel.tenantId = "9"
        uploadFileModel.category = "img"
        uploadFileModel.fileName = "blank-bucket.txt"
        uploadFileModel.inputStreamSource = InputStreamResource(ByteArrayInputStream("b".toByteArray()))

        val result = localUploadService.fileUpload(uploadFileModel)

        assertEquals("/9/img/blank-bucket.txt", result.filePath)
        assertTrue(Files.exists(tempDir.resolve("9/img/blank-bucket.txt")))
    }

    @Test
    fun fileUpload_reuses_existing_directory() {
        fun upload(name: String): String {
            val model = UploadFileModel<InputStreamResource>()
            model.fileSuffix = "txt"
            model.bucketName = "dup"
            model.tenantId = "1"
            model.category = "doc"
            model.fileName = name
            model.inputStreamSource = InputStreamResource(ByteArrayInputStream(name.toByteArray()))
            return checkNotNull(localUploadService.fileUpload(model).filePath)
        }

        val first = upload("first.txt")
        val second = upload("second.txt") // directory already exists -> createFileDir no-op branch

        assertTrue(Files.exists(tempDir.resolve(first.removePrefix("/"))))
        assertTrue(Files.exists(tempDir.resolve(second.removePrefix("/"))))
    }

    @Test
    fun fileUpload_rejects_bucketName_escaping_baseDir() {
        val uploadFileModel = UploadFileModel<InputStreamResource>()
        uploadFileModel.fileSuffix = "txt"
        uploadFileModel.bucketName = ".."
        uploadFileModel.tenantId = "0"
        uploadFileModel.category = "doc"
        uploadFileModel.fileName = "escape-dir.txt"
        uploadFileModel.inputStreamSource = InputStreamResource(ByteArrayInputStream("x".toByteArray()))

        val ex = assertFailsWith<ServiceException> {
            localUploadService.fileUpload(uploadFileModel)
        }
        assertEquals(FileErrorCode.FILE_UPLOAD_FAIL, ex.errorCode)
        // note: ServiceException does not preserve the cause chain (kudos-base issue); only errorCode is asserted
        assertFalse(Files.exists(tempDir.parent.resolve("0/doc/escape-dir.txt")))
    }

    @Test
    fun fileUpload_rejects_fileName_with_slash_or_backslash() {
        for (badName in listOf("a/b.txt", "a\\b.txt")) {
            val uploadFileModel = UploadFileModel<InputStreamResource>()
            uploadFileModel.fileSuffix = "txt"
            uploadFileModel.bucketName = "test"
            uploadFileModel.tenantId = "0"
            uploadFileModel.category = "doc"
            uploadFileModel.fileName = badName
            uploadFileModel.inputStreamSource = InputStreamResource(ByteArrayInputStream("x".toByteArray()))

            val ex = assertFailsWith<ServiceException> {
                localUploadService.fileUpload(uploadFileModel)
            }
            assertEquals(FileErrorCode.FILE_UPLOAD_FAIL, ex.errorCode)
            assertFalse(Files.exists(tempDir.resolve("test/0/doc/$badName")))
        }
    }

    @Test
    fun fileUpload_with_blank_fileName_generates_uuid_name() {
        val uploadFileModel = UploadFileModel<InputStreamResource>()
        uploadFileModel.fileSuffix = "txt"
        uploadFileModel.bucketName = "test"
        uploadFileModel.tenantId = "0"
        uploadFileModel.category = "doc"
        uploadFileModel.fileName = "   " // blank -> falls back to uuid + suffix
        uploadFileModel.inputStreamSource = InputStreamResource(ByteArrayInputStream("u".toByteArray()))

        val result = localUploadService.fileUpload(uploadFileModel)

        val filePath = checkNotNull(result.filePath)
        assertTrue(filePath.endsWith(".txt"))
        assertTrue(filePath.substringAfterLast('/').removeSuffix(".txt").isNotBlank())
        assertTrue(Files.exists(tempDir.resolve(filePath.removePrefix("/"))))
    }

    @Test
    fun fileUpload_rejects_driveRelative_fileName_escaping_baseDir() {
        // windows-only scenario: a drive-relative name like "Z:escape.txt" contains none of "..", "/", "\"
        // yet resolves outside baseDir, hitting the final startsWith(baseDir) guard
        assumeTrue(System.getProperty("os.name").lowercase().contains("win"))
        val baseDrive = tempDir.toAbsolutePath().root.toString().first().uppercaseChar()
        val otherDrive = if (baseDrive == 'Z') 'Y' else 'Z'

        val uploadFileModel = UploadFileModel<InputStreamResource>()
        uploadFileModel.fileSuffix = "txt"
        uploadFileModel.bucketName = "test"
        uploadFileModel.tenantId = "0"
        uploadFileModel.category = "doc"
        uploadFileModel.fileName = "$otherDrive:escape.txt"
        uploadFileModel.inputStreamSource = InputStreamResource(ByteArrayInputStream("x".toByteArray()))

        val ex = assertFailsWith<ServiceException> {
            localUploadService.fileUpload(uploadFileModel)
        }
        assertEquals(FileErrorCode.FILE_UPLOAD_FAIL, ex.errorCode)
    }

    @Test
    fun fileUpload_fails_when_inputStreamSource_is_null() {
        val uploadFileModel = UploadFileModel<InputStreamResource>()
        uploadFileModel.fileSuffix = "txt"
        uploadFileModel.bucketName = "test"
        uploadFileModel.tenantId = "0"
        uploadFileModel.category = "doc"
        uploadFileModel.fileName = "null-stream.txt"
        uploadFileModel.inputStreamSource = null

        val ex = assertFailsWith<ServiceException> {
            localUploadService.fileUpload(uploadFileModel)
        }
        assertEquals(FileErrorCode.FILE_UPLOAD_FAIL, ex.errorCode)
        assertFalse(Files.exists(tempDir.resolve("test/0/doc/null-stream.txt")))
    }

    @Test
    fun fileUpload_fails_when_basePath_not_set() {
        localProperties.basePath = null
        val uploadFileModel = UploadFileModel<InputStreamResource>()
        uploadFileModel.fileSuffix = "txt"
        uploadFileModel.bucketName = "test"
        uploadFileModel.tenantId = "0"
        uploadFileModel.category = "doc"
        uploadFileModel.inputStreamSource = InputStreamResource(ByteArrayInputStream("x".toByteArray()))

        val ex = assertFailsWith<IllegalArgumentException> {
            localUploadService.fileUpload(uploadFileModel)
        }
        assertTrue(ex.message!!.contains("base-path is not set"))
    }

}
