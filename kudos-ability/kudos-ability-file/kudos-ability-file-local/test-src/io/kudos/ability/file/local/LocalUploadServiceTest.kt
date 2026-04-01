package io.kudos.ability.file.local

import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.local.init.LocalUploadService
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.springframework.core.io.InputStreamResource
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 本地文件服务器上传操作测试用例
 *
 * @author unknown
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
internal class LocalUploadServiceTest {

    @Resource
    private lateinit var localUploadService: LocalUploadService


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
        assertTrue(checkNotNull(filename) { "filePath must not be null" }.contains(pathSnippet))
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
        assertTrue(checkNotNull(filename) { "filePath must not be null" }.contains(pathSnippet))
    }

}