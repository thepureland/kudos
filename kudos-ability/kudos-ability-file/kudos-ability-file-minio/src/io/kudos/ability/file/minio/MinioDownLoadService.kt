package io.kudos.ability.file.minio

import io.kudos.ability.file.common.AbstractDownLoadService
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.DownloadFileModel
import io.kudos.ability.file.minio.client.MinioClientBuilderFactory
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.minio.GetObjectArgs
import io.minio.GetObjectResponse
import io.minio.MinioClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier


open class MinioDownLoadService : AbstractDownLoadService() {

    @Autowired
    private lateinit var minioClientBuilderFactory: MinioClientBuilderFactory

    //静态客户端: 基于配置文件
    @Autowired
    @Qualifier("minioClient")
    private lateinit var minioClientDefault: MinioClient

    /**
     * 动态客户端: 基于认证参数
     *
     * @param model
     * @throws Exception
     */
    protected fun getMinioClient(model: DownloadFileModel<*>): MinioClient {
        if (model.authServerParam != null) {
            LOG.info(
                "Minio use auth server type:{0}",
                model.authServerParam!!.javaClass.getSimpleName()
            )
            return minioClientBuilderFactory.getInstance(model.authServerParam!!)!!.build()
        }
        return minioClientDefault
    }

    override fun readFileToByte(downloadFileModel: DownloadFileModel<*>): ByteArray? {
        val getArgs: GetObjectArgs? = GetObjectArgs
            .builder()
            .bucket(downloadFileModel.bucketName)
            .`object`(downloadFileModel.filePath)
            .build()
        try {
            val minioClient: MinioClient = getMinioClient(downloadFileModel)
            val `object`: GetObjectResponse? = minioClient.getObject(getArgs)
            if (`object` != null) {
                return `object`.readAllBytes()
            } else {
                throw ServiceException(FileErrorCode.FILE_NO_EXISTS)
            }
        } catch (e: io.minio.errors.ErrorResponseException) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_DENY, e)
        } catch (e: java.lang.Exception) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_ERROR, e)
        }
    }

    override fun readFileToStream(downloadFileModel: DownloadFileModel<*>): java.io.InputStream {
        val getArgs = GetObjectArgs
            .builder()
            .bucket(downloadFileModel.bucketName)
            .`object`(downloadFileModel.filePath)
            .build()
        try {
            val minioClient: MinioClient = getMinioClient(downloadFileModel)
            val obj = minioClient.getObject(getArgs)
            if (obj != null) {
                return obj
            } else {
                throw ServiceException(FileErrorCode.FILE_NO_EXISTS)
            }
        } catch (e: io.minio.errors.ErrorResponseException) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_DENY, e)
        } catch (e: java.lang.Exception) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_ERROR, e)
        }
    }

    private val LOG = LogFactory.getLog(this)

}
