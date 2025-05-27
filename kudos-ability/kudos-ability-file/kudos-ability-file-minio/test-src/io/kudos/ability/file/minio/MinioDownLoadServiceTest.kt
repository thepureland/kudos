package io.kudos.ability.file.minio

import io.kudos.test.common.EnableKudosTest
import org.junit.jupiter.api.*
import org.soul.ability.file.common.IDownLoadService
import org.soul.ability.file.common.IUploadService
import org.soul.ability.file.common.auth.AccessKeyServerParam
import org.soul.ability.file.common.auth.AccessTokenServerParam
import org.soul.ability.file.common.code.FileErrorCode
import org.soul.ability.file.common.entity.DownloadFileModel
import org.soul.ability.file.common.entity.UploadFileModel
import org.soul.ability.file.common.entity.UploadFileResult
import org.soul.base.exception.ServiceException
import org.soul.base.lang.string.RandomStringTool
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.testcontainers.junit.jupiter.Testcontainers

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
internal class MinioDownLoadServiceTest {
    @Autowired
    private val downLoadService: IDownLoadService? = null

    @Autowired
    private val uploadService: IUploadService? = null

    private var uploadFileResult: UploadFileResult? = null

    @BeforeAll
    private fun before_download() {
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
        this.uploadFileResult = uploadService!!.fileUpload(uploadFileModel)
    }

    @Test
    fun download_with_default_minio_client() {
        try {
            val downloadFileModel = DownloadFileModel.from(uploadFileResult!!.getFilePath())
            val rs = downLoadService!!.download(downloadFileModel)
            Assertions.assertTrue(rs.size > 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun download_with_specify_access_key_without_auth() {
        val downloadFileModel = DownloadFileModel.from(uploadFileResult!!.getFilePath())

        val accessKeyServerParam = AccessKeyServerParam()
        accessKeyServerParam.setAccessKey("test")
        accessKeyServerParam.setSecretKey("test")
        downloadFileModel.setAuthServerParam(accessKeyServerParam)

        try {
            val rs = downLoadService!!.download(downloadFileModel)
        } catch (e: ServiceException) {
            Assertions.assertTrue(e.getErrorCode() === FileErrorCode.FILE_ACCESS_DENY)
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
    @Disabled("依赖外部配置,请手工执行")
    @Test
    fun download_with_specify_access_token_with_auth() {
        //warning: 请确保bucket name : docs存在 ,且默认 private访问权限
        //warning: 请确保 fullPath变量指向的文件存在
        //warning: 请确认保headerValue的Session ID能通过安全认证,正常访问 AccessToken Endpoint

        val fullPath = "/docs/0/avatar.png"
        val downloadFileModel = DownloadFileModel.from(fullPath)

        val accessTokenServerParam = AccessTokenServerParam()
        accessTokenServerParam.setHeaderValue("e49f172d-e29c-44af-9f93-88c5a8285479") // 有效的SID
        downloadFileModel.setAuthServerParam(accessTokenServerParam)

        try {
            val rs = downLoadService!!.download(downloadFileModel)
            Assertions.assertTrue(rs.size > 0)
        } catch (e: Exception) {
            Assertions.assertNull(e)
        }
    }
}