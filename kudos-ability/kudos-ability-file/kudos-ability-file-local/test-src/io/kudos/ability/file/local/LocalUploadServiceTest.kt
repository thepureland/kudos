package io.kudos.ability.file.local

import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.local.init.LocalUploadService
import io.kudos.ability.file.local.init.properties.LocalProperties
import io.kudos.base.error.ServiceException
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.io.InputStreamResource
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
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

}
