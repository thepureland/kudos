package io.kudos.ability.file.minio

import io.kudos.base.lang.string.RandomStringKit
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.MinioTestContainer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestInstance
import org.soul.ability.file.common.IDownLoadService
import org.soul.ability.file.common.IUploadService
import org.soul.ability.file.common.auth.AccessKeyServerParam
import org.soul.ability.file.common.auth.AccessTokenServerParam
import org.soul.ability.file.common.code.FileErrorCode
import org.soul.ability.file.common.entity.DownloadFileModel
import org.soul.ability.file.common.entity.UploadFileModel
import org.soul.ability.file.common.entity.UploadFileResult
import org.soul.base.exception.ServiceException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.InputStreamSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import kotlin.test.Test
import kotlin.test.assertNull
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
@EnabledIfDockerAvailable
internal class MinioDownLoadServiceTest {

    @Autowired
    private lateinit var downLoadService: IDownLoadService

    @Autowired
    private lateinit var uploadService: IUploadService

    private var uploadFileResult: UploadFileResult? = null

    @BeforeAll
    fun before_download() {
        val bucketName = RandomStringKit.uuid()
        val tenantId = RandomStringKit.uuid()
        val resourceAsStream =
            MinioDownLoadServiceTest::class.java.getClassLoader().getResourceAsStream("files/test-file.txt")
        val uploadFileModel = UploadFileModel<InputStreamSource>()
        uploadFileModel.bucketName = bucketName
        uploadFileModel.category = "test"
        uploadFileModel.tenantId = tenantId
        uploadFileModel.fileSuffix = ".txt"
        uploadFileModel.inputStreamSource = InputStreamResource(resourceAsStream)
        this.uploadFileResult = uploadService.fileUpload(uploadFileModel)
    }

    @Test
    fun download_with_default_minio_client() {
        try {
            val downloadFileModel = DownloadFileModel.from(uploadFileResult!!.filePath)
            val rs = downLoadService.download(downloadFileModel)
            assertTrue(rs.size > 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun download_with_specify_access_key_without_auth() {
        val downloadFileModel = DownloadFileModel.from(uploadFileResult!!.filePath)

        val accessKeyServerParam = AccessKeyServerParam()
        accessKeyServerParam.accessKey = "test"
        accessKeyServerParam.secretKey = "test"
        downloadFileModel.authServerParam = accessKeyServerParam

        try {
            val rs = downLoadService.download(downloadFileModel)
        } catch (e: ServiceException) {
            assertTrue(e.errorCode === FileErrorCode.FILE_ACCESS_DENY)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
    @Test
    @Disabled("依赖外部配置,请手工执行")
    //TODO
    fun download_with_specify_access_token_with_auth() {
        //warning: 请确保bucket name : docs存在 ,且默认 private访问权限
        //warning: 请确保 fullPath变量指向的文件存在
        //warning: 请确认保headerValue的Session ID能通过安全认证,正常访问 AccessToken Endpoint

        val fullPath = "/docs/0/minio.png"
        val downloadFileModel = DownloadFileModel.from(fullPath)

        val accessTokenServerParam = AccessTokenServerParam()
        accessTokenServerParam.headerValue = "e49f172d-e29c-44af-9f93-88c5a8285479" // 有效的SID
        downloadFileModel.authServerParam = accessTokenServerParam

        try {
            val rs = downLoadService.download(downloadFileModel)
            assertTrue(rs.size > 0)
        } catch (e: Exception) {
            assertNull(e)
        }
    }

    companion object {

        @JvmStatic
        @DynamicPropertySource
        fun property(registry: DynamicPropertyRegistry) {
            MinioTestContainer.startIfNeeded(registry)
        }

    }

}