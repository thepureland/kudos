package io.kudos.ability.file.minio

import io.kudos.ability.file.common.IDeleteService
import io.kudos.ability.file.common.IUploadService
import io.kudos.ability.file.common.auth.AccessKeyServerParam
import io.kudos.ability.file.common.entity.DeleteFileModel
import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.base.error.ServiceException
import io.kudos.base.io.FileKit
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.MinioTestContainer
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.admin.MinioAdminClient
import io.minio.admin.UserInfo
import jakarta.annotation.Resource
import org.junit.jupiter.api.TestInstance
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.InputStreamSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MinIO 删除操作测试用例
 *
 * 设计目标：
 * 1. 覆盖路径合法性校验（非法路径直接返回 false，不抛异常）
 * 2. 覆盖“资源不存在”的异常分支（抛 ServiceException）
 * 3. 覆盖使用“默认凭证”的成功删除流程（先上传，再删除）
 * 4. 覆盖使用“指定 AccessKey/Secret”的成功删除流程（与默认凭证隔离）
 *
 * 测试环境准备（见 companion object#property）：
 * - 通过 Testcontainers 启动 MinIO（MinioTestContainer）
 * - 使用 root(admin/12345678)：
 *   a) 确保固定桶 docs 存在（避免 The specified bucket does not exist）
 *   b) 预置一个对象 path/to/__temp__.txt（便于本地手查；不影响用例逻辑）
 * - 通过 MinIO Admin API 创建仅删除用账号 delete_only_user，并绑定“delete-policy”
 * - 将 Spring 测试环境的默认凭证切换到 delete_only_user（便于覆盖“默认凭证成功删除”场景）
 *
 * 重要说明：
 * - 为避免 OIDC/STS 初始化时序与 503 问题，本测试仅使用 AccessKey/Secret，不涉及令牌换取流程
 * - “GetBucketLocation” 这类桶级元操作不要设置前缀条件，否则列桶/定位会失败（策略内已单独说明）
 *
 * @author ChatGPT
 * @author water
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfDockerAvailable
internal class MinioDeleteServiceTest {

    /** 业务侧上传服务，用于先上传一个对象以便后续删除 */
    @Resource
    private lateinit var uploadService: IUploadService

    /** 业务侧删除服务，被测对象 */
    @Resource
    private lateinit var deleteService: IDeleteService

    /**
     * 场景一：非法路径应被安全拒绝
     *
     * 期望：
     * - 传入包含“..”等穿越路径的 bucketName/filePath，delete() 返回 false（不中断、无异常）
     * - 体现业务侧对路径进行防注入/防穿越的前置校验
     */
    @Test
    fun delete_path_invalid() {
        val model = DeleteFileModel()

        // 非法 bucketName：仅“..”
        model.bucketName = ".."
        assertFalse(deleteService.delete(model))

        // 非法 bucketName：存在路径穿越
        model.bucketName = "a/../b"
        assertFalse(deleteService.delete(model))

        // 非法 filePath：仅“..”
        model.filePath = ".."
        assertFalse(deleteService.delete(model))

        // 再次非法 filePath：仅“..”（重复校验）
        model.filePath = ".."
        assertFalse(deleteService.delete(model))

        // 非法 filePath：路径穿越
        model.filePath = "a/../b"
        assertFalse(deleteService.delete(model))
    }

    /**
     * 场景二：资源不存在（桶/对象缺失）应抛业务异常
     *
     * 期望：
     * - 未提供 bucket 或 filePath 时，或提供了不存在的桶/对象，delete() 抛 ServiceException
     * - 体现“找不到资源”的错误通路由业务层统一处理
     */
    @Test
    fun delete_path_no_exist() {
        val model = DeleteFileModel()

        // 桶/路径都没填：抛异常
        assertFailsWith<ServiceException> { deleteService.delete(model) }

        // 桶不存在：抛异常
        model.bucketName = "no_exist"
        assertFailsWith<ServiceException> { deleteService.delete(model) }

        // 桶不存在 + 给一个路径：仍抛异常
        model.bucketName = "no_exist"
        model.filePath = "_"
        assertFailsWith<ServiceException> { deleteService.delete(model) }
    }

    /**
     * 场景三：使用“默认凭证”删除成功
     *
     * 步骤：
     * 1) 在本地 ~/.kudos 目录创建一个临时文件
     * 2) 通过 uploadService 上传到 MinIO（桶：docs）
     * 3) 删除本地临时文件（清理）
     * 4) 调用 deleteService.delete() 删除 MinIO 中的对象，期望返回 true
     *
     * 说明：
     * - 默认凭证在 @DynamicPropertySource 中被设置为 delete_only_user/delete_only_user_secret
     * - 该用户的策略允许对 docs 桶的对象进行读写删除（见下方策略）
     */
    @Test
    fun delete_success_default_auth_access_key() {
        val bucketName = "docs"
        val filePath = "__temp__.txt"
        val localPath = "${System.getProperty("user.home")}/.kudos/$filePath"

        // 1) 创建本地临时文件（若不存在则创建空文件）
        val localFile = File(localPath)
        FileKit.touch(localFile)

        // 2) 组织上传模型并上传到 MinIO
        val uploadFileModel = UploadFileModel<InputStreamSource>().apply {
            this.bucketName = bucketName
            this.fileSuffix = "txt"
            this.inputStreamSource = FileSystemResource(localFile)
        }
        val uploadFileResult = uploadService.fileUpload(uploadFileModel)
        assertFalse(uploadFileResult.filePath.isNullOrBlank())

        // 3) 删除本地临时文件（仅做清理，不影响远端对象）
        FileKit.forceDelete(localFile)

        // 4) 删除 MinIO 中的对象
        val model = DeleteFileModel.from(uploadFileResult.filePath!!)
        assertTrue(deleteService.delete(model))
    }

    /**
     * 场景四：显式指定 AccessKey/Secret 删除成功
     *
     * 步骤与上一个用例类似，不同点在于：
     * - 在 DeleteFileModel 上显式指定账号 delete_only_user/delete_only_user_secret
     * - 以覆盖“调用者以不同凭证执行删除”的行为路径
     *
     * 说明：
     * - 早期备注里提示“需要手工创建用户并授权”，但本测试已在 @DynamicPropertySource 中自动完成创建与授权
     */
    @Test
    fun delete_success_specify_auth_access_key() {
        val bucketName = "docs"
        val filePath = "__temp__.txt"
        val localPath = "${System.getProperty("user.home")}/.kudos/$filePath"

        // 1) 创建本地临时文件
        val localFile = File(localPath)
        FileKit.touch(localFile)

        // 2) 上传到 MinIO
        val uploadFileModel = UploadFileModel<InputStreamSource>().apply {
            this.bucketName = bucketName
            this.fileSuffix = "txt"
            this.inputStreamSource = FileSystemResource(localFile)
        }
        val uploadFileResult = uploadService.fileUpload(uploadFileModel)
        assertFalse(uploadFileResult.filePath.isNullOrBlank())

        // 3) 删除本地文件
        FileKit.forceDelete(localFile)

        // 4) 指定“仅删除用户”的凭证并删除 MinIO 对象
        val model = DeleteFileModel.from(uploadFileResult.filePath!!)
        model.authServerParam = AccessKeyServerParam(DELETE_ONLY_USER, DELETE_ONLY_USER_SECRET)
        assertTrue(deleteService.delete(model))
    }

    /**
     * 测试环境引导（在 Spring Context 刚创建时执行）：
     *
     * - 启动/复用 MinIO Testcontainer
     * - 把 Spring 的默认访问凭证设置为 delete_only_user（覆盖默认行为）
     * - 用 root(admin/12345678) 确保：
     *   a) docs 桶存在
     *   b) 预置一个对象 path/to/__temp__.txt（方便排查）
     * - 通过 MinIO Admin API：
     *   a) 创建 delete-policy（允许对 docs 的对象读写删除 + 桶元操作）
     *   b) 创建 delete_only_user 并绑定策略
     *
     * 备注：
     * - “s3:GetBucketLocation/ ListBucket/ ListBucketVersions” 作用于桶级资源（arn:aws:s3:::docs），
     *   不能对它们再加对象级前缀条件，否则会破坏列桶/定位
     */
    companion object {
        private const val ROOT_USER = "admin"
        private const val ROOT_USER_SECRET = "12345678"
        private const val DELETE_ONLY_USER = "delete_only_user"
        private const val DELETE_ONLY_USER_SECRET = "delete_only_user_secret"

        @JvmStatic
        @DynamicPropertySource
        fun property(registry: DynamicPropertyRegistry) {
            // 1) 启动或复用 MinIO 容器，并把 Spring 的默认凭证切换到“仅删除用户”
            val minio = MinioTestContainer.startIfNeeded(registry)
            registry.add("kudos.ability.file.minio.accessKey") { DELETE_ONLY_USER }
            registry.add("kudos.ability.file.minio.secretKey") { DELETE_ONLY_USER_SECRET }

            // 2) 计算对外访问地址
            val address = "http://${minio.ports.first().ip}:${minio.ports.first().publicPort}"
            val bucket = "docs"     // 固定桶（避免“桶不存在”问题）
            val prefix = "path/to/" // 示例前缀（便于区分/检视），注意以 / 结尾

            // 3) 用 root 账户确保 bucket 存在，并塞入一个示例对象（便于人工核查）
            val rootClient = MinioClient.builder()
                .endpoint(address)
                .credentials(ROOT_USER, ROOT_USER_SECRET)
                .build()

            if (!rootClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                rootClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
            }

            rootClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .`object`("${prefix}__temp__.txt")
                    .stream(ByteArrayInputStream("__temp__".toByteArray()), -1, 5 * 1024 * 1024)
                    .contentType("text/plain")
                    .build()
            )

            // 4) 管理端创建策略 + 用户 + 绑定策略
            val admin = MinioAdminClient.builder()
                .endpoint(address)
                .credentials(ROOT_USER, ROOT_USER_SECRET)
                .build()

            // 注意：桶级动作（GetBucketLocation/ListBucket/ListBucketVersions）绑定到桶 ARN（无 /*）
            // 对象级动作（Get/Put/DeleteObject 等）绑定到对象 ARN（带 /*）
            val policyJson = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Sid": "BucketMetaAndList",
                  "Effect": "Allow",
                  "Action": [ "s3:GetBucketLocation", "s3:ListBucket", "s3:ListBucketVersions" ],
                  "Resource": [ "arn:aws:s3:::docs" ]
                },
                {
                  "Sid": "ObjectRWAll",
                  "Effect": "Allow",
                  "Action": [
                    "s3:GetObject", "s3:PutObject", "s3:DeleteObject",
                    "s3:GetObjectVersion", "s3:DeleteObjectVersion",
                    "s3:AbortMultipartUpload"
                  ],
                  "Resource": [ "arn:aws:s3:::docs/*" ]
                }
              ]
            }
            """.trimIndent()

            // 创建策略（若存在会抛冲突，此处保留原逻辑；如需幂等可改为 runCatching 包裹）
            admin.addCannedPolicy("delete-policy", policyJson)

            // 创建用户（若存在则忽略冲突异常）
            runCatching {
                admin.addUser(
                    DELETE_ONLY_USER,
                    UserInfo.Status.ENABLED,
                    DELETE_ONLY_USER_SECRET,
                    null,
                    null
                )
            }

            // 绑定策略到用户
            admin.setPolicy(DELETE_ONLY_USER, false, "delete-policy")
        }
    }
}
