package io.kudos.ability.file.minio

import io.kudos.ability.file.common.AbstractDownLoadService
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.DownloadFileModel
import io.kudos.ability.file.minio.client.MinioClientBuilderFactory
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.minio.GetObjectArgs
import io.minio.MinioClient
import java.io.InputStream
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
        val auth = model.authServerParam ?: return minioClientDefault
        LOG.info("Minio use auth server type:{0}", auth.javaClass.simpleName)
        return requireNotNull(minioClientBuilderFactory.getInstance(auth)) { "MinioClient builder not found" }.build()
    }

    override fun readFileToByte(downloadFileModel: DownloadFileModel<*>): ByteArray? {
        val getArgs = GetObjectArgs.builder()
            .bucket(downloadFileModel.bucketName)
            .`object`(downloadFileModel.filePath)
            .build()
        return try {
            getMinioClient(downloadFileModel).getObject(getArgs).use { it.readAllBytes() }
        } catch (e: io.minio.errors.ErrorResponseException) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_DENY, e)
        } catch (e: Exception) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_ERROR, e)
        }
    }

    override fun readFileToStream(downloadFileModel: DownloadFileModel<*>): InputStream {
        val getArgs = GetObjectArgs.builder()
            .bucket(downloadFileModel.bucketName)
            .`object`(downloadFileModel.filePath)
            .build()
        return try {
            getMinioClient(downloadFileModel).getObject(getArgs)
        } catch (e: io.minio.errors.ErrorResponseException) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_DENY, e)
        } catch (e: Exception) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_ERROR, e)
        }
    }

    private val LOG = LogFactory.getLog(this::class)

}
