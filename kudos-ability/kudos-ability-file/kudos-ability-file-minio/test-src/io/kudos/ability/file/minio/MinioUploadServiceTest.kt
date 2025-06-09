package io.kudos.ability.file.minio

import io.kudos.base.lang.string.RandomStringKit
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.MinioTestContainer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestInstance
import org.soul.ability.file.common.IUploadService
import org.soul.ability.file.common.auth.AccessKeyServerParam
import org.soul.ability.file.common.auth.AccessTokenServerParam
import org.soul.ability.file.common.code.FileErrorCode
import org.soul.ability.file.common.entity.UploadFileModel
import org.soul.base.exception.ServiceException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.InputStreamSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * minio下载操作测试用例
 *
 * @author water
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
internal class MinioUploadServiceTest {

    @Autowired
    private lateinit var uploadService: IUploadService

    /**
     * 测试条件
     * 1) minioClient 使用默认高级用户
     *
     *
     * 测试参数:
     * 1) 随机bucket name
     * 2) 随机商户ID
     */
    @Test
    fun fileUpload_with_default_minio_client() {
        val bucketName = RandomStringKit.uuid()
        val tenantId = RandomStringKit.uuid()
        val resourceAsStream =
            MinioUploadServiceTest::class.java.getClassLoader().getResourceAsStream("files/test-file.txt")
        val uploadFileModel = UploadFileModel<InputStreamSource>()
        uploadFileModel.bucketName = bucketName
        uploadFileModel.category = "test"
        uploadFileModel.tenantId = tenantId
        uploadFileModel.fileSuffix = ".txt"
        uploadFileModel.inputStreamSource = InputStreamResource(resourceAsStream)
        val uploadFileResult = uploadService.fileUpload(uploadFileModel)
        assertTrue(uploadFileResult.filePath.startsWith("/$bucketName"))
        assertTrue(uploadFileResult.filePath.contains(tenantId))
        assertTrue(uploadFileResult.filePath.endsWith(".txt"))
    }

    /**
     * 测试条件
     * 1) minioClient 使用未存在的 test用户
     *
     *
     * 测试参数:
     * 1) 随机bucket name
     * 2) 随机商户ID
     */
    @Test
    fun fileUpload_with_specify_access_key_without_auth() {
        val bucketName = RandomStringKit.uuid()
        val tenantId = RandomStringKit.uuid()
        val resourceAsStream =
            MinioUploadServiceTest::class.java.getClassLoader().getResourceAsStream("files/test-file.txt")
        val uploadFileModel = UploadFileModel<InputStreamSource>()
        uploadFileModel.bucketName = bucketName
        uploadFileModel.category = "test"
        uploadFileModel.tenantId = tenantId
        uploadFileModel.fileSuffix = ".txt"
        uploadFileModel.inputStreamSource = InputStreamResource(resourceAsStream)

        val accessKeyServerParam = AccessKeyServerParam()
        accessKeyServerParam.accessKey = "test"
        accessKeyServerParam.secretKey = "test"
        uploadFileModel.authServerParam = accessKeyServerParam

        val se = assertFailsWith<ServiceException> { uploadService.fileUpload(uploadFileModel) }
        assertEquals(se.errorCode, FileErrorCode.FILE_ACCESS_DENY)
    }

    /**
     * 测试条件
     * 1) minioClient 无效的资源中心Session ID( Header Value) [AccessTokenServerProperties.getEndpoint]
     *
     *
     * 测试参数:
     * 1) 指定bucketName
     * 2) 指定tenantId
     * 3) 随机headerValue
     */
    @Disabled("依赖外部配置,请手工执行")
    @Test
    fun fileUpload_with_specify_access_token_without_auth() {
        //minio 预设 docs
        val bucketName = "docs"
        val tenantId = "0"
        val resourceAsStream =
            MinioUploadServiceTest::class.java.getClassLoader().getResourceAsStream("files/test-file.txt")
        val uploadFileModel = UploadFileModel<InputStreamSource>()
        uploadFileModel.bucketName = bucketName
        uploadFileModel.category = "test"
        uploadFileModel.tenantId = tenantId
        uploadFileModel.fileSuffix = ".txt"
        uploadFileModel.inputStreamSource = InputStreamResource(resourceAsStream)

        val accessTokenServerParam = AccessTokenServerParam()
        accessTokenServerParam.headerValue = RandomStringKit.uuid() //无效AccessToken
        uploadFileModel.authServerParam = accessTokenServerParam

        val se = assertFailsWith<ServiceException> { uploadService.fileUpload(uploadFileModel)        }
        assertEquals(se.errorCode, FileErrorCode.FILE_ACCESS_DENY)
    }

    /**
     * 测试条件<br></br>
     * 1) minioClient 无效的资源中心Session ID( Header Value) [AccessTokenServerProperties.getEndpoint]<br></br>
     * 2) minio 配置 Policy
     * 3) AccessToken Endpoint端点
     *
     *
     * 测试参数:
     * 1) 指定bucketName
     * 2) 指定tenantId
     * 3) 指定headerValue
     */
    @Disabled("依赖外部配置,请手工执行")
    @Test
    fun fileUpload_with_specify_access_token_with_auth() {
        //minio 预设 docs
        val bucketName = "docs"
        val tenantId = "0"
        val resourceAsStream =
            MinioUploadServiceTest::class.java.getClassLoader().getResourceAsStream("files/test-file.txt")
        val uploadFileModel = UploadFileModel<InputStreamSource>()
        uploadFileModel.bucketName = bucketName
        uploadFileModel.category = "test"
        uploadFileModel.tenantId = "0"
        uploadFileModel.fileSuffix = ".txt"
        uploadFileModel.inputStreamSource = InputStreamResource(resourceAsStream)

        val accessTokenServerParam = AccessTokenServerParam()
        accessTokenServerParam.headerValue = "2b7639c1-8527-4ab7-9e26-5dd55d1fae27" // 有效的SID
        uploadFileModel.authServerParam = accessTokenServerParam

        val uploadFileResult = uploadService.fileUpload(uploadFileModel)
        assertTrue(uploadFileResult.filePath.startsWith("/$bucketName"))
        assertTrue(uploadFileResult.filePath.endsWith(".txt"))
    }

    companion object {

        @JvmStatic
        @DynamicPropertySource
        fun property(registry: DynamicPropertyRegistry) {
            MinioTestContainer.start(registry)
        }

    }

}