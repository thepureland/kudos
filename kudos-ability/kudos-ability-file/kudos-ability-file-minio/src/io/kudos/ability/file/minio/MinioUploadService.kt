package io.kudos.ability.file.minio

import io.kudos.ability.file.common.AbstractUploadService
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.compress.CompressionPipeline
import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.minio.client.MinioClientBuilderFactory
import io.kudos.ability.file.minio.init.properties.MinioProperties
import io.kudos.base.error.ServiceException
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.base.logger.LogFactory
import io.minio.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import java.io.ByteArrayInputStream


open class MinioUploadService : AbstractUploadService() {
    
    @Autowired
    private lateinit var properties: MinioProperties

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
    protected fun getMinioClient(model: UploadFileModel<*>): MinioClient {
        if (model.authServerParam != null) {
            LOG.info(
                "Minio use auth server type:{0}",
                model.authServerParam!!.javaClass.getSimpleName()
            )
            return minioClientBuilderFactory.getInstance(model.authServerParam!!)!!.build()
        }
        return minioClientDefault
    }

    protected fun createBucket(minioClient: MinioClient, model: UploadFileModel<*>) {
        if (model.authServerParam != null) {
            // warning: 动态权限时,默认bucket需要手工创建.
            // todo:  minioClient.bucketExists 的需要哪个policy?
            return
        }
        val bucketArg = BucketExistsArgs.builder().bucket(model.bucketName).build()
        if (!minioClient.bucketExists(bucketArg)) {
            val makeBucketArgs = MakeBucketArgs.builder()
                .bucket(model.bucketName)
                .build()
            minioClient.makeBucket(makeBucketArgs)
            //设置匿名访问
            //setPolicy(minioClient, model.getBucketName());
        }
    }

    private fun setPolicy(minioClient: MinioClient, bucketName: String?) {
        // 下发匿名读策略
        val policyJson = "{\n" +
                "  \"Version\": \"2025-07-02\",\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Principal\": { \"AWS\": [\"*\"] },\n" +
                "      \"Action\": [\"s3:GetBucketLocation\",\"s3:ListBucket\"],\n" +
                "      \"Resource\": [\"arn:aws:s3:::" + bucketName + "\"]\n" +
                "    },\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Principal\": { \"AWS\": [\"*\"] },\n" +
                "      \"Action\": [\"s3:GetObject\"],\n" +
                "      \"Resource\": [\"arn:aws:s3:::" + bucketName + "/*\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}"
        minioClient.setBucketPolicy(
            SetBucketPolicyArgs.builder()
                .bucket(bucketName)
                .config(policyJson)
                .build()
        )
    }

    override fun saveFile(model: UploadFileModel<*>, fileDir: String): String {
        try {
            val minioClient: MinioClient = getMinioClient(model)
            createBucket(minioClient, model)
            var fName = model.fileName
            if (fName.isNullOrBlank()) {
                fName = RandomStringKit.uuid() + "." + model.fileSuffix
            }
            val fullFilePath = "$fileDir/$fName"

            val inputStream = model.inputStreamSource!!.inputStream
            val result = CompressionPipeline.compress(inputStream, fullFilePath, model.compressionConfig)

            val putArgs = PutObjectArgs.builder()
                .bucket(model.bucketName)
                .`object`(result.getOutputFilePath())
                .stream(
                    if (result.outputStream == null) inputStream else ByteArrayInputStream(
                        result.outputStream!!.toByteArray()
                    ), -1, 10485760
                )
                .contentType(result.mimeType).build()

            val rs = minioClient.putObject(putArgs)
            //访问路径包含: bucketName,方便业务直接存储,前端拼接绝对http地址
            return "/" + rs.bucket() + "/" + rs.`object`()
        } catch (e: io.minio.errors.ErrorResponseException) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_DENY, e)
        } catch (e: java.lang.Exception) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_ERROR, e)
        }
    }

    override fun pathPrefix(): String {
        return properties.publicEndpoint!!
    }

    private val LOG = LogFactory.getLog(this)

}
