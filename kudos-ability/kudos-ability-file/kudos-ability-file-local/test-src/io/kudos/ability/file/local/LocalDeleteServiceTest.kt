package io.kudos.ability.file.local

import io.kudos.test.common.EnableKudosTest
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.soul.ability.file.common.IDeleteService
import org.soul.ability.file.common.entity.DeleteFileModel
import org.soul.ability.file.local.starter.properties.LocalProperties
import org.soul.base.exception.ServiceException
import org.soul.base.io.FileTool
import java.io.File

@EnableKudosTest
internal class LocalDeleteServiceTest {
    @Resource
    private val deleteService: IDeleteService? = null

    @Resource
    private val localProperties: LocalProperties? = null

    @Test
    fun delete_path_invalid() {
        val model = DeleteFileModel()
        model.setBucketName("..")
        Assertions.assertFalse(deleteService!!.delete(model))

        model.setBucketName("a/../b")
        Assertions.assertFalse(deleteService.delete(model))

        model.setFilePath("..")
        Assertions.assertFalse(deleteService.delete(model))

        model.setFilePath("..")
        Assertions.assertFalse(deleteService.delete(model))

        model.setFilePath("a/../b")
        Assertions.assertFalse(deleteService.delete(model))
    }

    @Test
    fun delete_path_folder() {
        val bucketName = "__"
        val filePath = "__"
        val fullPath = localProperties!!.getBasePath() + "/" + bucketName + "/" + filePath
        FileTool.forceMkdir(File(fullPath))

        val model = DeleteFileModel()
        model.setBucketName(bucketName)
        model.setFilePath(filePath)
        Assertions.assertFalse(deleteService!!.delete(model))
    }

    @Test
    fun delete_path_no_exist() {
        val model = DeleteFileModel()
        Assertions.assertThrows<ServiceException?>(
            ServiceException::class.java,
            Executable { deleteService!!.delete(model) })

        model.setBucketName("_")
        Assertions.assertThrows<ServiceException?>(
            ServiceException::class.java,
            Executable { deleteService!!.delete(model) })

        model.setFilePath("_")
        Assertions.assertThrows<ServiceException?>(
            ServiceException::class.java,
            Executable { deleteService!!.delete(model) })
    }

    @Test
    fun delete_path_success() {
        val bucketName = "__"
        val filePath = "__temp__.txt"
        val fullPath = localProperties!!.getBasePath() + "/" + bucketName + "/" + filePath
        FileTool.touch(File(fullPath))

        val model = DeleteFileModel()
        model.setBucketName(bucketName)
        model.setFilePath(filePath)
        Assertions.assertTrue(deleteService!!.delete(model))
    }
}