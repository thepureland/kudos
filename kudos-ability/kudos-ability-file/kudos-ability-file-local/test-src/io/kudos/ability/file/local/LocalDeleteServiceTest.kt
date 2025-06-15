package io.kudos.ability.file.local

import io.kudos.base.io.FileKit
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.soul.ability.file.common.IDeleteService
import org.soul.ability.file.common.entity.DeleteFileModel
import org.soul.ability.file.local.starter.properties.LocalProperties
import org.soul.base.exception.ServiceException
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 本地文件服务器删除操作测试用例
 *
 * @author unknown
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
internal class LocalDeleteServiceTest {

    @Resource
    private lateinit var deleteService: IDeleteService

    @Resource
    private lateinit var localProperties: LocalProperties

    @Test
    fun delete_path_invalid() {
        val model = DeleteFileModel()
        model.bucketName = ".."
        assertFalse(deleteService.delete(model))

        model.bucketName = "a/../b"
        assertFalse(deleteService.delete(model))

        model.filePath = ".."
        assertFalse(deleteService.delete(model))

        model.filePath = ".."
        assertFalse(deleteService.delete(model))

        model.filePath = "a/../b"
        assertFalse(deleteService.delete(model))
    }

    @Test
    fun delete_path_folder() {
        val bucketName = "__"
        val filePath = "__"
        val fullPath = "${localProperties.basePath}/$bucketName/$filePath"
        FileKit.forceMkdir(File(fullPath))

        val model = DeleteFileModel()
        model.bucketName = bucketName
        model.filePath = filePath
        assertFalse(deleteService.delete(model))
    }

    @Test
    fun delete_path_no_exist() {
        val model = DeleteFileModel()
        assertFailsWith<ServiceException> { deleteService.delete(model) }

        model.bucketName = "_"
        assertFailsWith<ServiceException> { deleteService.delete(model) }

        model.filePath = "_"
        assertFailsWith<ServiceException> { deleteService.delete(model) }
    }

    @Test
    fun delete_path_success() {
        val bucketName = "__"
        val filePath = "__temp__.txt"
        val fullPath = "${localProperties.basePath}/$bucketName/$filePath"
        FileKit.touch(File(fullPath))

        val model = DeleteFileModel()
        model.bucketName = bucketName
        model.filePath = filePath
        assertTrue(deleteService.delete(model))
    }

}