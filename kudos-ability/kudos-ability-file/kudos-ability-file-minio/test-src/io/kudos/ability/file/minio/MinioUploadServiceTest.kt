package io.kudos.ability.file.minio

import io.kudos.ability.file.common.IUploadService
import io.kudos.ability.file.common.auth.AccessKeyServerParam
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.base.error.ServiceException
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.MinioTestContainer
import io.minio.admin.MinioAdminClient
import io.minio.admin.UserInfo
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import jakarta.annotation.Resource
import org.junit.jupiter.api.TestInstance
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.InputStreamSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * minio下载操作测试用例
 *
 * 说明：
 * 1) 不使用 OIDC / STS，仅通过 AccessKey/Secret 与 MinIO 交互，避免 503/初始化时序问题
 * 2) Admin 账户在启动时创建两个测试用户：
 *    - uploader         ：全局读写 + 建桶（作为 Spring 默认凭证）
 *    - upload_only_user ：仅上传 + 桶元操作 + 建桶（用于“指定 AccessKey 成功上传”的场景）
 * 3) 为避免“The specified bucket does not exist”，所有用例统一使用固定桶：docs，
 *    并在初始化时用 root 账号确保 docs 已创建（同 MinioDeleteServiceTest 的思路）。
 *
 * 测试项：
 * - fileUpload_with_default_minio_client           ：默认凭证上传成功（bucket=docs）
 * - fileUpload_with_specify_access_key_without_auth：指定错误凭证被拒绝（FILE_ACCESS_DENY）
 * - fileUpload_with_specify_access_key_with_auth   ：指定写入账号上传成功（bucket=docs）
 *
 * @author ChatGPT
 * @author water
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
@EnabledIfDockerAvailable
internal class MinioUploadServiceTest {

    @Resource
    private lateinit var uploadService: IUploadService

    /**
     * 测试条件
     * 1) minioClient 使用默认高级用户
     *
     *
     * 测试参数:
     * 1) 固定 bucket name = docs（已在初始化时创建）
     * 2) 随机商户ID
     */
    @Test
    fun fileUpload_with_default_minio_client() {
        val bucketName = "docs"
        val tenantId = RandomStringKit.uuid()
        val resourceAsStream =
            MinioUploadServiceTest::class.java.classLoader.getResourceAsStream("files/test-file.txt")
                ?: "hello-minio".byteInputStream()

        val uploadFileModel = UploadFileModel<InputStreamSource>().apply {
            this.bucketName = bucketName
            this.category = "test"
            this.tenantId = tenantId
            this.fileSuffix = ".txt"
            this.inputStreamSource = InputStreamResource(resourceAsStream)
        }

        val uploadFileResult = uploadService.fileUpload(uploadFileModel)
        assertTrue(uploadFileResult.filePath!!.startsWith("/$bucketName"))
        assertTrue(uploadFileResult.filePath!!.contains(tenantId))
        assertTrue(uploadFileResult.filePath!!.endsWith(".txt"))
    }

    /**
     * 测试条件
     * 1) minioClient 使用未存在的 test用户
     *
     *
     * 测试参数:
     * 1) 固定 bucket name = docs（已在初始化时创建）
     * 2) 随机商户ID
     */
    @Test
    fun fileUpload_with_specify_access_key_without_auth() {
        val bucketName = "docs"
        val tenantId = RandomStringKit.uuid()
        val resourceAsStream =
            MinioUploadServiceTest::class.java.classLoader.getResourceAsStream("files/test-file.txt")
                ?: "hello-minio".byteInputStream()

        val uploadFileModel = UploadFileModel<InputStreamSource>().apply {
            this.bucketName = bucketName
            this.category = "test"
            this.tenantId = tenantId
            this.fileSuffix = ".txt"
            this.inputStreamSource = InputStreamResource(resourceAsStream)
        }

        // 故意给错的 accessKey/secretKey
        uploadFileModel.authServerParam = AccessKeyServerParam("test", "test")

        val se = assertFailsWith<ServiceException> { uploadService.fileUpload(uploadFileModel) }
        assertEquals(se.errorCode, FileErrorCode.FILE_ACCESS_DENY)
    }

    /**
     * 测试条件
     * 1) 指定具备上传权限的 AccessKey/Secret（upload_only_user）
     *
     *
     * 测试参数:
     * 1) 固定 bucket name = docs（已在初始化时创建）
     * 2) 随机 tenantId
     */
    @Test
    fun fileUpload_with_specify_access_key_with_auth() {
        val bucketName = "docs"
        val tenantId = RandomStringKit.uuid()
        val resourceAsStream =
            MinioUploadServiceTest::class.java.classLoader.getResourceAsStream("files/test-file.txt")
                ?: "hello-minio".byteInputStream()

        val uploadFileModel = UploadFileModel<InputStreamSource>().apply {
            this.bucketName = bucketName
            this.category = "test"
            this.tenantId = tenantId
            this.fileSuffix = ".txt"
            this.inputStreamSource = InputStreamResource(resourceAsStream)
        }

        // 指定“仅上传用户”的 AccessKey/Secret
        uploadFileModel.authServerParam = AccessKeyServerParam(UPLOAD_ONLY_USER, UPLOAD_ONLY_USER_SECRET)

        val uploadFileResult = uploadService.fileUpload(uploadFileModel)
        assertTrue(uploadFileResult.filePath!!.startsWith("/$bucketName"))
        assertTrue(uploadFileResult.filePath!!.endsWith(".txt"))
    }

    companion object {
        // ---- Root 管理员（来自 MinioTestContainer 的缺省 root）----
        private const val ROOT_USER = "admin"
        private const val ROOT_USER_SECRET = "12345678"

        // ---- 默认上传用户（用于 Spring 默认配置）----
        private const val DEFAULT_UPLOADER = "uploader"
        private const val DEFAULT_UPLOADER_SECRET = "uploader_secret"

        // ---- 仅上传用户（用于“指定 AccessKey 成功上传”）----
        private const val UPLOAD_ONLY_USER = "upload_only_user"
        private const val UPLOAD_ONLY_USER_SECRET = "upload_only_user_secret"

        @JvmStatic
        @DynamicPropertySource
        fun property(registry: DynamicPropertyRegistry) {
            val minio = MinioTestContainer.startIfNeeded(registry)
            val endpoint = "http://${minio.ports.first().ip}:${minio.ports.first().publicPort}"

            // 让 Spring 使用我们创建的默认上传用户
            registry.add("kudos.ability.file.minio.endpoint") { endpoint }
            registry.add("kudos.ability.file.minio.public-endpoint") { endpoint }
            registry.add("kudos.ability.file.minio.accessKey") { DEFAULT_UPLOADER }
            registry.add("kudos.ability.file.minio.secretKey") { DEFAULT_UPLOADER_SECRET }

            // 1) 先确保固定桶 docs 已存在（避免 The specified bucket does not exist）
            val rootClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(ROOT_USER, ROOT_USER_SECRET)
                .build()
            val bucket = "docs"
            val exists = rootClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
            )
            if (!exists) {
                rootClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
            }

            // 2) 用 root 管理员创建策略 + 用户
            val admin = MinioAdminClient.builder()
                .endpoint(endpoint)
                .credentials(ROOT_USER, ROOT_USER_SECRET)
                .build()

            // 默认上传用户：全桶读写 + 建桶（便于扩展）
            val uploaderPolicy = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Action": [
                    "s3:GetBucketLocation",
                    "s3:ListBucket",
                    "s3:ListBucketVersions",
                    "s3:CreateBucket"
                  ],
                  "Resource": [ "arn:aws:s3:::*" ]
                },
                {
                  "Effect": "Allow",
                  "Action": [
                    "s3:GetObject",
                    "s3:PutObject",
                    "s3:DeleteObject",
                    "s3:GetObjectVersion",
                    "s3:DeleteObjectVersion",
                    "s3:AbortMultipartUpload"
                  ],
                  "Resource": [ "arn:aws:s3:::*/*" ]
                }
              ]
            }
            """.trimIndent()

            // 仅上传用户：允许建桶 + 桶元操作 + PutObject（本用例只需 docs 已存在，但策略保留）
            val uploadOnlyPolicy = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Action": [
                    "s3:GetBucketLocation",
                    "s3:ListBucket",
                    "s3:ListBucketVersions",
                    "s3:CreateBucket"
                  ],
                  "Resource": [ "arn:aws:s3:::*" ]
                },
                {
                  "Effect": "Allow",
                  "Action": [
                    "s3:PutObject",
                    "s3:AbortMultipartUpload"
                  ],
                  "Resource": [ "arn:aws:s3:::*/*" ]
                }
              ]
            }
            """.trimIndent()

            // 创建策略（若已存在可忽略异常）
            runCatching { admin.addCannedPolicy("uploader-rw-all", uploaderPolicy) }
            runCatching { admin.addCannedPolicy("upload-only-all", uploadOnlyPolicy) }

            // 创建用户（若已存在可忽略异常）
            runCatching {
                admin.addUser(DEFAULT_UPLOADER, UserInfo.Status.ENABLED, DEFAULT_UPLOADER_SECRET, null, null)
            }
            runCatching {
                admin.addUser(UPLOAD_ONLY_USER, UserInfo.Status.ENABLED, UPLOAD_ONLY_USER_SECRET, null, null)
            }

            // 绑定策略
            admin.setPolicy(DEFAULT_UPLOADER, false, "uploader-rw-all")
            admin.setPolicy(UPLOAD_ONLY_USER, false, "upload-only-all")
        }
    }
}
