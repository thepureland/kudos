package io.kudos.ability.file.minio

import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.MinioTestContainer
import io.minio.admin.MinioAdminClient
import io.minio.admin.UserInfo
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.soul.ability.file.common.IDeleteService
import org.soul.ability.file.common.IUploadService
import org.soul.ability.file.common.auth.AccessKeyServerParam
import org.soul.ability.file.common.entity.DeleteFileModel
import org.soul.ability.file.common.entity.UploadFileModel
import org.soul.base.exception.ServiceException
import org.soul.base.io.FileTool
import org.soul.base.lang.string.StringTool
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.InputStreamSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File


/**
 * minio删除操作测试用例
 *
 * @author water
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
internal class MinioDeleteServiceTest {

    @Resource
    private lateinit var uploadService: IUploadService

    @Resource
    private lateinit var deleteService: IDeleteService


    @Test
    fun delete_path_invalid() {
        val model = DeleteFileModel()
        model.bucketName = ".."
        Assertions.assertFalse(deleteService.delete(model))

        model.bucketName = "a/../b"
        Assertions.assertFalse(deleteService.delete(model))

        model.filePath = ".."
        Assertions.assertFalse(deleteService.delete(model))

        model.filePath = ".."
        Assertions.assertFalse(deleteService.delete(model))

        model.filePath = "a/../b"
        Assertions.assertFalse(deleteService.delete(model))
    }

    @Test
    fun delete_path_no_exist() {
        val model = DeleteFileModel()
        Assertions.assertThrows(ServiceException::class.java) { deleteService.delete(model) }

        model.bucketName = "no_exist"
        Assertions.assertThrows(ServiceException::class.java) { deleteService.delete(model) }

        model.bucketName = "no_exist"
        model.filePath = "_"
        Assertions.assertThrows(ServiceException::class.java) { deleteService.delete(model) }
    }

    /**
     * 使用配置的用户名密码
     */
    @Test
    fun delete_success_default_auth_access_key() {
        val bucketName = "docs"
        val filePath = "__temp__.txt"
        val localPath = "${System.getProperty("user.home")}/.soul/$filePath"

        //create local file
        val localFile = File(localPath)
        FileTool.touch(localFile)

        val uploadFileModel = UploadFileModel<InputStreamSource>()
        uploadFileModel.bucketName = bucketName
        uploadFileModel.fileSuffix = "txt"
        uploadFileModel.inputStreamSource = FileSystemResource(localFile)

        //upload to minio
        val uploadFileResult = uploadService.fileUpload(uploadFileModel)
        Assertions.assertTrue(StringTool.isNotBlank(uploadFileResult.filePath))

        //delete local file
        FileTool.forceDelete(localFile)

        //delete minio file
        val model = DeleteFileModel.from(uploadFileResult.filePath)
        Assertions.assertTrue(deleteService.delete(model))
    }

    /**
     * 场景: 指定Minio 用户名与密码进行文件删除
     *
     * 其中用户帐号: delete_only_user
     * 其中用户密码: delete_only_user_secret
     * 其中Minio Policy权限须知,并授权给上述用户
     * 使用配置的用户名密码
     */
    @Test
    fun delete_success_specify_auth_access_key() {
        val bucketName = "docs"
        val filePath = "__temp__.txt"
        val localPath = "${System.getProperty("user.home")}/.soul/$filePath"

        //create local file
        val localFile = File(localPath)
        FileTool.touch(localFile)

        val uploadFileModel = UploadFileModel<InputStreamSource>()
        uploadFileModel.bucketName = bucketName
        uploadFileModel.fileSuffix = "txt"
        uploadFileModel.inputStreamSource = FileSystemResource(localFile)

        //upload to minio
        val uploadFileResult = uploadService.fileUpload(uploadFileModel)
        Assertions.assertTrue(StringTool.isNotBlank(uploadFileResult.filePath))

        //delete local file
        FileTool.forceDelete(localFile)

        //delete minio file
        val model = DeleteFileModel.from(uploadFileResult.filePath)
        //指定用户名
        //waring: minio需要手工创建以下用户,并授权
        model.authServerParam = AccessKeyServerParam(DELETE_ONLY_USER, DELETE_ONLY_USER_SECRET)
        Assertions.assertTrue(deleteService.delete(model))
    }

    companion object {
        private const val ROOT_USER = "admin"
        private const val ROOT_USER_SECRET = "12345678"
        private const val DELETE_ONLY_USER = "delete_only_user"
        private const val DELETE_ONLY_USER_SECRET = "delete_only_user_secret"

        @JvmStatic
        @DynamicPropertySource
        fun property(registry: DynamicPropertyRegistry) {
            val minio = MinioTestContainer.start(registry)
            val address = "http://${minio.host}:${minio.getMappedPort(9000)}"

            // 1. 构建 Admin 客户端
            val adminClient = MinioAdminClient.builder()
                .endpoint(address)
                .credentials(ROOT_USER, ROOT_USER_SECRET)
                .build()

            // 2. 创建一个 delete-only 策略（只针对 mybucket/path/to/* 下对象）
            val policyName = "delete-policy"
            val policyJson = """
              {
                "Version":"2012-10-17",
                "Statement":[
                  {
                    "Effect":"Allow",
                    "Action":["s3:DeleteObject","s3:GetBucketLocation","s3:GetObject"],
                    "Resource":["arn:aws:s3:::docs/*"]
                  }
                ]
              }
            """.trimIndent()
            adminClient.addCannedPolicy(policyName, policyJson)


            // 3. 创建新用户
            adminClient.addUser(
                DELETE_ONLY_USER,              // accessKey
                UserInfo.Status.ENABLED,       // 状态
                DELETE_ONLY_USER_SECRET,       // secretKey
                null,                          // policyName，null 表示暂时不绑定策略
                null                           // memberOf，null 表示不加入任何组
            )

            // 4. 把策略绑定到刚才那个用户
            adminClient.setPolicy(
                DELETE_ONLY_USER,    // userOrGroupName
                false,               // isGroup = false（表示这是用户而不是用户组）
                policyName           // 之前定义的策略名称
            )
        }

    }

}