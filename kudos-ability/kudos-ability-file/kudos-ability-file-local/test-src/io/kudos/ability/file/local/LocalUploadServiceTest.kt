package io.kudos.ability.file.local

import io.kudos.test.common.EnableKudosTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.soul.ability.file.common.entity.UploadFileModel
import org.soul.ability.file.local.LocalUploadService
import org.soul.ability.file.local.starter.properties.LocalProperties
import org.soul.base.exception.CustomRuntimeException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import java.lang.String
import java.util.*
import kotlin.Any
import kotlin.Array
import kotlin.arrayOf
import kotlin.text.contains
import kotlin.text.startsWith

@EnableKudosTest
internal class LocalUploadServiceTest {
    @Autowired
    var localProperties: LocalProperties? = null

    @Autowired
    var localUploadService: LocalUploadService? = null

    @Test
    fun saveFile_null_inputStream() {
        val uploadFileModel: UploadFileModel<*> = UploadFileModel<Any?>()
        uploadFileModel.setFileSuffix("txt")
        uploadFileModel.setBucketName("test")
        uploadFileModel.setTenantId("0")
        Assertions.assertThrows<CustomRuntimeException?>(CustomRuntimeException::class.java, Executable {
            localUploadService.saveFile(uploadFileModel, FILE_MID_DIR)
        })
    }

    @Test
    fun saveFile_filename() {
        val resourceAsStream =
            LocalUploadServiceTest::class.java.getClassLoader().getResourceAsStream("files/test-file.txt")
        val uploadFileModel: UploadFileModel<*> = UploadFileModel<Any?>()
        uploadFileModel.setFileSuffix("txt")
        uploadFileModel.setBucketName("test")
        uploadFileModel.setTenantId("0")
        uploadFileModel.setInputStreamSource(InputStreamResource(resourceAsStream))

        val filename = localUploadService.saveFile(uploadFileModel, FILE_MID_DIR)
        Assertions.assertFalse(filename.startsWith(localProperties!!.getBasePath()))
        Assertions.assertTrue(filename.startsWith("/" + uploadFileModel.getBucketName()))
    }

    @Test
    fun fileUpload_with_category() {
        val resourceAsStream =
            LocalUploadServiceTest::class.java.getClassLoader().getResourceAsStream("files/test-file.txt")
        val uploadFileModel: UploadFileModel<*> = UploadFileModel<Any?>()
        uploadFileModel.setFileSuffix("txt")
        uploadFileModel.setBucketName("test")
        uploadFileModel.setTenantId("0")
        uploadFileModel.setCategory("doc")
        uploadFileModel.setInputStreamSource(InputStreamResource(resourceAsStream))

        val uploadFileResult = localUploadService!!.fileUpload(uploadFileModel)
        val filename = uploadFileResult.getFilePath()

        val arr: Array<String?> = arrayOf<String>("0", "doc")
        Assertions.assertTrue(filename.contains(String.join("/", *arr)))
    }

    @Test
    fun fileUpload_without_category() {
        val resourceAsStream =
            LocalUploadServiceTest::class.java.getClassLoader().getResourceAsStream("files/test-file.txt")
        val uploadFileModel: UploadFileModel<*> = UploadFileModel<Any?>()
        uploadFileModel.setFileSuffix("txt")
        uploadFileModel.setBucketName("test")
        uploadFileModel.setTenantId("0")
        uploadFileModel.setInputStreamSource(InputStreamResource(resourceAsStream))
        val uploadFileResult = localUploadService!!.fileUpload(uploadFileModel)
        val filename = uploadFileResult.getFilePath()

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val arr: Array<kotlin.String?> = arrayOf<kotlin.String>("0", year.toString(), month.toString(), day.toString())
        Assertions.assertTrue(filename.contains(String.join("/", *arr)))
    }

    companion object {
        const val FILE_MID_DIR: kotlin.String = "soul-ability-file-local"
    }
}