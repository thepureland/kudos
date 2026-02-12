package io.kudos.ability.file.minio

import io.kudos.ability.file.common.IDeleteService
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.DeleteFileModel
import io.kudos.ability.file.minio.client.MinioClientBuilderFactory
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.minio.MinioClient
import io.minio.RemoveObjectArgs
import io.minio.StatObjectArgs
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier


open class MinioDeleteService : IDeleteService {

    @Autowired
    private lateinit var minioClientBuilderFactory: MinioClientBuilderFactory

    //静态客户端: 基于配置文件
    @Autowired
    @Qualifier("minioClient")
    private val minioClientDefault: MinioClient? = null


    /**
     * 动态客户端: 基于认证参数
     *
     * @param model 请求参数
     * @throws Exception 异常
     */
    protected fun getMinioClient(model: DeleteFileModel): MinioClient? {
        if (model.authServerParam != null) {
            LOG.info(
                "Minio use auth server type:{0}",
                requireNotNull(model.authServerParam) { "authServerParam is null" }.javaClass.getSimpleName()
            )
            return requireNotNull(minioClientBuilderFactory.getInstance(requireNotNull(model.authServerParam) { "authServerParam is null" })) { "MinioClient builder not found" }.build()
        }
        return minioClientDefault
    }

    /**
     * @param model 请求路径
     * @return
     */
    override fun delete(model: DeleteFileModel): Boolean {
        if (!isValid(model)) {
            return false
        }

        try {
            //判定文件在存在
            requireNotNull(getMinioClient(model)) { "MinioClient is null" }
                .statObject(
                    StatObjectArgs.builder()
                        .bucket(model.bucketName)
                        .`object`(model.filePath)
                        .build()
                )

            //具体删除
            requireNotNull(getMinioClient(model)) { "MinioClient is null" }.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(model.bucketName)
                    .`object`(model.filePath)
                    .build()
            )
            return true
        } catch (e: io.minio.errors.ErrorResponseException) {
            val code = e.errorResponse().code()
            if ("NoSuchKey" == code || "NoSuchBucket" == code) {
                throw ServiceException(FileErrorCode.FILE_NO_EXISTS, e)
            } else if ("InvalidAccessKeyId" == code) {
                throw ServiceException((FileErrorCode.FILE_INVALID_ACCESS_KEY))
            } else if ("AccessDenied" == code) {
                throw ServiceException((FileErrorCode.FILE_ACCESS_DENY))
            }
            throw ServiceException(FileErrorCode.FILE_DELETE_FAIL, e)
        } catch (e: java.lang.Exception) {
            throw ServiceException(FileErrorCode.FILE_DELETE_FAIL, e)
        }
    }

    private val LOG = LogFactory.getLog(this)

}
