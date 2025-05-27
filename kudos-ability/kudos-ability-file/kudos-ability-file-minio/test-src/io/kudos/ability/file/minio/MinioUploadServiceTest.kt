package io.kudos.ability.file.minio

import io.kudos.test.common.EnableKudosTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.soul.ability.file.common.IUploadService
import org.soul.ability.file.common.auth.AccessKeyServerParam
import org.soul.ability.file.common.auth.AccessTokenServerParam
import org.soul.ability.file.common.code.FileErrorCode
import org.soul.ability.file.common.entity.UploadFileModel
import org.soul.base.exception.ServiceException
import org.soul.base.lang.string.RandomStringTool
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource

@EnableKudosTest
@Disabled("minio未被testcontainer初始化")
internal class MinioUploadServiceTest {
    @Autowired
    private val uploadService: IUploadService? = null

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
        val bucketName = RandomStringTool.uuid()
        val tenantId = RandomStringTool.uuid()
        val resourceAsStream =
            MinioUploadServiceTest::class.java.getClassLoader().getResourceAsStream("files/test-file.txt")
        val uploadFileModel: UploadFileModel<*> = UploadFileModel<Any?>()
        uploadFileModel.setBucketName(bucketName)
        uploadFileModel.setCategory("test")
        uploadFileModel.setTenantId(tenantId)
        uploadFileModel.setFileSuffix(".txt")
        uploadFileModel.setInputStreamSource(InputStreamResource(resourceAsStream))
        val uploadFileResult = uploadService!!.fileUpload(uploadFileModel)
        Assertions.assertTrue(uploadFileResult.getFilePath().startsWith("/" + bucketName))
        Assertions.assertTrue(uploadFileResult.getFilePath().contains(tenantId))
        Assertions.assertTrue(uploadFileResult.getFilePath().endsWith(".txt"))
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
        val bucketName = RandomStringTool.uuid()
        val tenantId = RandomStringTool.uuid()
        val resourceAsStream =
            MinioUploadServiceTest::class.java.getClassLoader().getResourceAsStream("files/test-file.txt")
        val uploadFileModel: UploadFileModel<*> = UploadFileModel<Any?>()
        uploadFileModel.setBucketName(bucketName)
        uploadFileModel.setCategory("test")
        uploadFileModel.setTenantId(tenantId)
        uploadFileModel.setFileSuffix(".txt")
        uploadFileModel.setInputStreamSource(InputStreamResource(resourceAsStream))

        val accessKeyServerParam = AccessKeyServerParam()
        accessKeyServerParam.setAccessKey("test")
        accessKeyServerParam.setSecretKey("test")
        uploadFileModel.setAuthServerParam(accessKeyServerParam)

        val se = Assertions.assertThrows<ServiceException>(ServiceException::class.java, Executable {
            val uploadFileResult = uploadService!!.fileUpload(uploadFileModel)
        })
        Assertions.assertEquals(se.getErrorCode(), FileErrorCode.FILE_ACCESS_DENY)
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
        val uploadFileModel: UploadFileModel<*> = UploadFileModel<Any?>()
        uploadFileModel.setBucketName(bucketName)
        uploadFileModel.setCategory("test")
        uploadFileModel.setTenantId(tenantId)
        uploadFileModel.setFileSuffix(".txt")
        uploadFileModel.setInputStreamSource(InputStreamResource(resourceAsStream))

        val accessTokenServerParam = AccessTokenServerParam()
        accessTokenServerParam.setHeaderValue(RandomStringTool.uuid()) //无效AccessToken
        uploadFileModel.setAuthServerParam(accessTokenServerParam)

        val se = Assertions.assertThrows<ServiceException>(ServiceException::class.java, Executable {
            val uploadFileResult = uploadService!!.fileUpload(uploadFileModel)
        })
        Assertions.assertEquals(se.getErrorCode(), FileErrorCode.FILE_ACCESS_DENY)
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
        val uploadFileModel: UploadFileModel<*> = UploadFileModel<Any?>()
        uploadFileModel.setBucketName(bucketName)
        uploadFileModel.setCategory("test")
        uploadFileModel.setTenantId("0")
        uploadFileModel.setFileSuffix(".txt")
        uploadFileModel.setInputStreamSource(InputStreamResource(resourceAsStream))

        val accessTokenServerParam = AccessTokenServerParam()
        accessTokenServerParam.setHeaderValue("2b7639c1-8527-4ab7-9e26-5dd55d1fae27") // 有效的SID
        uploadFileModel.setAuthServerParam(accessTokenServerParam)

        val uploadFileResult = uploadService!!.fileUpload(uploadFileModel)
        Assertions.assertTrue(uploadFileResult.getFilePath().startsWith("/" + bucketName))
        Assertions.assertTrue(uploadFileResult.getFilePath().endsWith(".txt"))
    }
}