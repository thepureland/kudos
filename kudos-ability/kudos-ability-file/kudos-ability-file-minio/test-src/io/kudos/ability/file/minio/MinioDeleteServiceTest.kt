package io.kudos.ability.file.minio

import io.kudos.test.common.EnableKudosTest
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.soul.ability.file.common.IDeleteService
import org.soul.ability.file.common.IUploadService
import org.soul.ability.file.common.auth.AccessKeyServerParam
import org.soul.ability.file.common.entity.DeleteFileModel
import org.soul.ability.file.common.entity.UploadFileModel
import org.soul.base.exception.ServiceException
import org.soul.base.io.FileTool
import org.soul.base.lang.string.StringTool
import org.springframework.core.io.FileSystemResource
import java.io.File
import java.io.UnsupportedEncodingException

@EnableKudosTest
internal class MinioDeleteServiceTest {
    @Resource
    private val uploadService: IUploadService? = null

    @Resource
    private val deleteService: IDeleteService? = null

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
    fun delete_path_no_exist() {
        val model = DeleteFileModel()
        Assertions.assertThrows<ServiceException?>(
            ServiceException::class.java,
            Executable { deleteService!!.delete(model) })

        model.setBucketName("no_exist")
        Assertions.assertThrows<ServiceException?>(
            ServiceException::class.java,
            Executable { deleteService!!.delete(model) })

        model.setBucketName("no_exist")
        model.setFilePath("_")
        Assertions.assertThrows<ServiceException?>(
            ServiceException::class.java,
            Executable { deleteService!!.delete(model) })
    }

    /**
     * 使用配置的用户名密码
     * @throws UnsupportedEncodingException
     */
    @Test
    @Throws(UnsupportedEncodingException::class)
    fun delete_success_default_auth_access_key() {
        val bucketName = "docs"
        val filePath = "__temp__.txt"
        val localPath = System.getProperty("user.home") + "/.soul/" + filePath

        //create local file
        val localFile = File(localPath)
        FileTool.touch(localFile)

        val uploadFileModel: UploadFileModel<*> = UploadFileModel<Any?>()
        uploadFileModel.setBucketName(bucketName)
        uploadFileModel.setFileSuffix("txt")
        uploadFileModel.setInputStreamSource(FileSystemResource(localFile))

        //upload to minio
        val uploadFileResult = uploadService!!.fileUpload(uploadFileModel)
        Assertions.assertTrue(StringTool.isNotBlank(uploadFileResult.getFilePath()))

        //delete local file
        FileTool.forceDelete(localFile)

        //delete minio file
        val model = DeleteFileModel.from(uploadFileResult.getFilePath())
        Assertions.assertTrue(deleteService!!.delete(model))
    }

    /**
     * 场景: 指定Minio 用户名与密码进行文件删除
     *
     *
     * 其中用户帐号: delete_only_user
     * 其中用户密码: delete_only_user
     *
     *
     *
     * 其中Minio Policy权限须知,并授权给上述用户
     * `
     * {
     * "Version": "2012-10-17",
     * "Statement": [
     * {
     * "Effect": "Allow",
     * "Action": [
     * "s3:DeleteObject",
     * "s3:GetBucketLocation",
     * "s3:GetObject"
     * ],
     * "Resource": [
     * "arn:aws:s3:::docs/ *"
     * ]
     * }
     * ]
     * }
    ` *
     *
     * 使用配置的用户名密码
     * @throws UnsupportedEncodingException
     */
    @Test
    @Throws(UnsupportedEncodingException::class)
    fun delete_success_specify_auth_access_key() {
        val bucketName = "docs"
        val filePath = "__temp__.txt"
        val localPath = System.getProperty("user.home") + "/.soul/" + filePath

        //create local file
        val localFile = File(localPath)
        FileTool.touch(localFile)

        val uploadFileModel: UploadFileModel<*> = UploadFileModel<Any?>()
        uploadFileModel.setBucketName(bucketName)
        uploadFileModel.setFileSuffix("txt")
        uploadFileModel.setInputStreamSource(FileSystemResource(localFile))

        //upload to minio
        val uploadFileResult = uploadService!!.fileUpload(uploadFileModel)
        Assertions.assertTrue(StringTool.isNotBlank(uploadFileResult.getFilePath()))

        //delete local file
        FileTool.forceDelete(localFile)

        //delete minio file
        val model = DeleteFileModel.from(uploadFileResult.getFilePath())
        //指定用户名
        //waring: minio需要手工创建以下用户,并授权
        model.setAuthServerParam(AccessKeyServerParam("delete_only_user", "delete_only_user"))
        Assertions.assertTrue(deleteService!!.delete(model))
    }
}