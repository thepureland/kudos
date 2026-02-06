package io.kudos.ability.file.local

import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.local.init.LocalUploadService
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.springframework.core.io.InputStreamResource
import java.lang.String
import java.util.*
import kotlin.arrayOf
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.text.contains

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

        val arr = arrayOf("0", "doc")
        assertTrue(filename!!.contains(String.join("/", *arr)))
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

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val arr = arrayOf("0", year.toString(), month.toString(), day.toString())
        assertTrue(filename!!.contains(String.join("/", *arr)))
    }

}