package io.kudos.ability.file.minio

import io.kudos.ability.file.common.IDownLoadService
import io.kudos.ability.file.common.IUploadService
import io.kudos.ability.file.common.auth.AccessKeyServerParam
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.DownloadFileModel
import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.common.entity.UploadFileResult
import io.kudos.base.error.ServiceException
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.MinioTestContainer
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.admin.MinioAdminClient
import io.minio.admin.UserInfo
import jakarta.annotation.Resource
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.InputStreamSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * minio下载操作测试用例
 *
 * 说明：
 * 1) 不使用 OIDC / STS，只用 AccessKey/Secret 跟 MinIO 交互，避免 503/初始化时序问题
 * 2) Admin 账户在启动时创建两个测试用户：
 *    - rw_user：docs 桶下对象读写（供上传 + 默认下载）
 *    - ro_user：docs 桶下对象只读（用于“指定 AccessKey 成功下载”的场景）
 * 3) 测试项：
 *    - download_with_default_minio_client：使用默认凭证下载（默认凭证配置成 rw_user）
 *    - download_with_specify_access_key_without_auth：指定错误凭证，期望抛出 FILE_ACCESS_DENY
 *    - download_with_specify_access_key_with_auth：指定 ro_user 凭证，期望下载成功
 *
 * @author ChatGPT
 * @author water
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfDockerInstalled
internal class MinioDownLoadServiceTest {

    @Resource
    private lateinit var downLoadService: IDownLoadService

    @Resource
    private lateinit var uploadService: IUploadService

    private var uploadFileResult: UploadFileResult? = null

    @BeforeAll
    fun before_download() {
        // 统一固定到 docs 桶，避免创建随机桶需要额外的“建桶权限”
        val bucketName = BUCKET
        val tenantId = RandomStringKit.uuid()

        val resourceAsStream =
            MinioDownLoadServiceTest::class.java.classLoader.getResourceAsStream("files/test-file.txt")
                ?: ByteArrayInputStream("hello-minio".toByteArray())

        val uploadFileModel = UploadFileModel<InputStreamSource>().apply {
            this.bucketName = bucketName
            this.category = "test"
            this.tenantId = tenantId
            this.fileSuffix = ".txt"
            this.inputStreamSource = InputStreamResource(resourceAsStream)
        }
        this.uploadFileResult = uploadService.fileUpload(uploadFileModel)
        require(!uploadFileResult?.filePath.isNullOrBlank()) { "upload failed: filePath is null/blank" }
    }

    /**
     * 使用配置的用户名密码（默认：rw_user）下载
     */
    @Test
    fun download_with_default_minio_client() {
        val downloadFileModel = DownloadFileModel.from(uploadFileResult!!.filePath!!)
        val rs = downLoadService.download(downloadFileModel)
        assertTrue(rs!!.isNotEmpty(), "default download should return bytes")
    }

    /**
     * 指定错误的 AccessKey/Secret，期望被拒绝（FILE_ACCESS_DENY）
     */
    @Test
    fun download_with_specify_access_key_without_auth() {
        val downloadFileModel = DownloadFileModel.from(uploadFileResult!!.filePath!!)

        // 故意给错的 accessKey/secretKey
        downloadFileModel.authServerParam = AccessKeyServerParam("bad", "bad")

        try {
            downLoadService.download(downloadFileModel)
            kotlin.test.fail("expected ServiceException(FileErrorCode.FILE_ACCESS_DENY)")
        } catch (e: ServiceException) {
            assertSame(e.errorCode, FileErrorCode.FILE_ACCESS_DENY)
        }
    }

    /**
     * 指定只读用户 ro_user 的 AccessKey/Secret 进行下载
     * 期望成功（ro_user 策略仅允许 GetObject）
     */
    @Test
    fun download_with_specify_access_key_with_auth() {
        val downloadFileModel = DownloadFileModel.from(uploadFileResult!!.filePath!!)
        downloadFileModel.authServerParam = AccessKeyServerParam(RO_USER, RO_USER_SECRET)

        val rs = downLoadService.download(downloadFileModel)
        assertTrue(rs!!.isNotEmpty(), "download with ro_user should return bytes")
    }

    companion object {
        // ---- Root 管理员 ----
        private const val ROOT_USER = "admin"
        private const val ROOT_USER_SECRET = "12345678"

        // ---- 读写账户（默认凭证，用于上传 + 默认下载）----
        private const val RW_USER = "rw_user"
        private const val RW_USER_SECRET = "rw_user_secret"

        // ---- 只读账户（用于“指定凭证下载成功”）----
        private const val RO_USER = "ro_user"
        private const val RO_USER_SECRET = "ro_user_secret"

        // 固定桶名，方便在策略里精确授权
        private const val BUCKET = "docs"

        @JvmStatic
        @DynamicPropertySource
        fun property(registry: DynamicPropertyRegistry) {
            // 启动容器并注册 endpoint 属性
            val minio = MinioTestContainer.startIfNeeded(registry)
            val address = "http://${minio.ports.first().ip}:${minio.ports.first().publicPort}"

            // 1) 用 root 凭证准备环境：创建 docs 桶 + 两个策略 + 两个用户 + 绑策略
            prepareMinioForTests(address)

            // 2) Spring 环境默认凭证：配置为 rw_user（用于 @BeforeAll 的上传动作 & 默认下载）
            registry.add("kudos.ability.file.minio.endpoint") { address }
            registry.add("kudos.ability.file.minio.public-endpoint") { address }
            registry.add("kudos.ability.file.minio.accessKey") { RW_USER }
            registry.add("kudos.ability.file.minio.secretKey") { RW_USER_SECRET }
        }

        /**
         * 用 root 管理员准备测试环境：
         * - 创建 docs 桶（如果不存在）
         * - 创建两个策略：rw-policy（读写），ro-policy（只读）
         * - 创建两个用户：rw_user、ro_user
         * - 将策略分别绑定到用户
         */
        private fun prepareMinioForTests(address: String) {
            // ---- data 面客户端（用 root 创建桶/放对象时也可用）----
            val rootClient = MinioClient.builder().endpoint(address).credentials(ROOT_USER, ROOT_USER_SECRET).build()

            // 确保桶存在
            val exists = rootClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())
            if (!exists) {
                rootClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build())
            }

            // ---- admin 面客户端（创建策略/用户/授权）----
            val admin = MinioAdminClient.builder().endpoint(address).credentials(ROOT_USER, ROOT_USER_SECRET).build()

            // 读写策略：允许 docs 桶元信息 + docs/* 对象的读写（方便上传/下载）
            val rwPolicy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Sid": "BucketMeta",
                      "Effect": "Allow",
                      "Action": [ "s3:GetBucketLocation", "s3:ListBucket", "s3:ListBucketVersions" ],
                      "Resource": [ "arn:aws:s3:::$BUCKET" ]
                    },
                    {
                      "Sid": "ObjectRW",
                      "Effect": "Allow",
                      "Action": [
                        "s3:GetObject","s3:PutObject","s3:DeleteObject",
                        "s3:GetObjectVersion","s3:DeleteObjectVersion","s3:AbortMultipartUpload"
                      ],
                      "Resource": [ "arn:aws:s3:::$BUCKET/*" ]
                    }
                  ]
                }
            """.trimIndent()

            // 只读策略：允许 docs 桶元信息 + docs/* 对象的读取
            val roPolicy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Sid": "BucketMeta",
                      "Effect": "Allow",
                      "Action": [ "s3:GetBucketLocation", "s3:ListBucket", "s3:ListBucketVersions" ],
                      "Resource": [ "arn:aws:s3:::$BUCKET" ]
                    },
                    {
                      "Sid": "ObjectRead",
                      "Effect": "Allow",
                      "Action": [ "s3:GetObject","s3:GetObjectVersion" ],
                      "Resource": [ "arn:aws:s3:::$BUCKET/*" ]
                    }
                  ]
                }
            """.trimIndent()

            // 创建/覆盖策略（若存在 addCannedPolicy 可能报冲突，可用 runCatching 忽略）
            runCatching { admin.addCannedPolicy("rw-policy", rwPolicy) }
            runCatching { admin.addCannedPolicy("ro-policy", roPolicy) }

            // 创建用户（若已存在可忽略异常）
            runCatching { admin.addUser(RW_USER, UserInfo.Status.ENABLED, RW_USER_SECRET, null, null) }
            runCatching { admin.addUser(RO_USER, UserInfo.Status.ENABLED, RO_USER_SECRET, null, null) }

            // 绑定策略
            admin.setPolicy(RW_USER, false, "rw-policy")
            admin.setPolicy(RO_USER, false, "ro-policy")

            // 也可以提前放一个对象，供“非上传路径”的下载测试使用（这里不是必须）
            runCatching {
                rootClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(BUCKET)
                        .`object`("seed/__hello__.txt")
                        .stream(ByteArrayInputStream("seed".toByteArray()), -1, 5 * 1024 * 1024)
                        .contentType("text/plain")
                        .build()
                )
            }
        }
    }
}
