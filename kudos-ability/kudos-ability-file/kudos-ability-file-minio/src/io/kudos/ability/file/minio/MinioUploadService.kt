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


/**
 * MinIO（S3 兼容）文件上传服务。
 *
 * 客户端选取策略：[UploadFileModel.authServerParam] 为空时复用配置文件装的静态 `minioClient` bean；
 * 非空时通过 [MinioClientBuilderFactory] 按认证类型（AK/SK 或 OAuth token）现场构造客户端。
 *
 * 上传前会调用 [createBucket] 自动建桶——仅在静态客户端模式下；动态认证模式下假定 bucket 已存在
 * （动态权限多半没有 `s3:CreateBucket` 权限，访问 `bucketExists` 也可能 403）。
 *
 * @author K
 * @since 1.0.0
 */
open class MinioUploadService : AbstractUploadService() {

    /** MinIO 配置（endpoint / accessKey / secretKey / publicEndpoint 等） */
    @Autowired
    private lateinit var properties: MinioProperties

    /** 动态认证场景下用来构造对应类型的 MinioClient builder */
    @Autowired
    private lateinit var minioClientBuilderFactory: MinioClientBuilderFactory

    /** 静态客户端：来自 `kudos.ability.file.minio.{endpoint,accessKey,secretKey}` 装配。 */
    @Autowired
    @Qualifier("minioClient")
    private lateinit var minioClientDefault: MinioClient

    /**
     * 按 [UploadFileModel.authServerParam] 决定使用静态 / 动态客户端。
     *
     * @param model 上传请求；`authServerParam` 为空时用默认静态客户端
     * @throws IllegalArgumentException 找不到对应认证类型的 builder
     */
    protected fun getMinioClient(model: UploadFileModel<*>): MinioClient {
        val auth = model.authServerParam ?: return minioClientDefault
        LOG.info("Minio use auth server type:{0}", auth.javaClass.simpleName)
        return requireNotNull(minioClientBuilderFactory.getInstance(auth)) { "MinioClient builder not found" }.build()
    }

    /**
     * 桶不存在则创建（仅静态客户端模式）。
     *
     * 动态认证场景下直接返回——动态颁发的 token 通常没有 `s3:CreateBucket` 权限，连
     * `bucketExists` 都可能 403。**这要求业务侧提前在 MinIO 控制台预创建所有用到的 bucket**，
     * 否则后续 `putObject` 会拿到 `NoSuchBucket` 错误。
     *
     * 注：旧实现这里有一段 `setPolicy(...)` 配置匿名读策略的代码，但其 `Version` 字段写成了
     * `"2025-07-02"`（标准应当是 AWS IAM 的 `"2012-10-17"`），MinIO / S3 会拒收；另外开匿名
     * 读不是所有部署都接受。该死代码已移除——bucket policy 应通过 MinIO 控制台 / mc 命令
     * 显式配置，避免应用层悄悄开放公网读。
     */
    protected fun createBucket(minioClient: MinioClient, model: UploadFileModel<*>) {
        if (model.authServerParam != null) {
            return
        }
        val bucketArg = BucketExistsArgs.builder().bucket(model.bucketName).build()
        if (!minioClient.bucketExists(bucketArg)) {
            val makeBucketArgs = MakeBucketArgs.builder()
                .bucket(model.bucketName)
                .build()
            minioClient.makeBucket(makeBucketArgs)
        }
    }

    /**
     * 上传文件到 MinIO/S3。
     *
     * 流程：选择客户端 → 必要时建桶 → 走压缩管道 → `putObject`。
     * 流大小传 -1 表示未知，由 SDK 内部按 partSize（10MB）切片上传；这是 MinIO 客户端处理"未知大小流"的标准用法。
     * 返回的路径包含 bucketName，便于业务侧直接存库后由前端拼 publicEndpoint 形成完整 URL。
     *
     * @param model 上传请求
     * @param fileDir [AbstractUploadService.dispatchFileDir] 分配出来的相对目录
     * @return `/{bucket}/{objectKey}` 形态的路径
     * @throws ServiceException 失败时按错误类型分两个错误码：
     *   - [FileErrorCode.FILE_ACCESS_DENY]：[io.minio.errors.ErrorResponseException]（鉴权/权限/桶不存在等）
     *   - [FileErrorCode.FILE_ACCESS_ERROR]：其它本地或网络异常
     * @author K
     * @since 1.0.0
     */
    override fun saveFile(model: UploadFileModel<*>, fileDir: String): String {
        try {
            val minioClient: MinioClient = getMinioClient(model)
            createBucket(minioClient, model)
            val fName = model.fileName?.takeUnless { it.isBlank() }
                ?: "${RandomStringKit.uuid()}.${model.fileSuffix}"
            val fullFilePath = "$fileDir/$fName"

            val inputStream = requireNotNull(model.inputStreamSource) { "inputStreamSource is null" }.inputStream
            val result = CompressionPipeline.compress(inputStream, fullFilePath, model.compressionConfig)

            val uploadStream = result.outputStream?.let { ByteArrayInputStream(it.toByteArray()) } ?: inputStream
            val putArgs = PutObjectArgs.builder()
                .bucket(model.bucketName)
                .`object`(result.getOutputFilePath())
                .stream(uploadStream, -1, 10485760)
                .contentType(result.mimeType).build()

            val rs = minioClient.putObject(putArgs)
            //访问路径包含: bucketName,方便业务直接存储,前端拼接绝对http地址
            return "/${rs.bucket()}/${rs.`object`()}"
        } catch (e: io.minio.errors.ErrorResponseException) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_DENY, e)
        } catch (e: Exception) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_ERROR, e)
        }
    }

    /**
     * 返回 MinIO 公网域名，前端把它拼到 [saveFile] 的相对路径前形成完整 URL。
     *
     * @return [MinioProperties.publicEndpoint]
     * @throws IllegalArgumentException 当配置中未指定 publicEndpoint 时
     * @author K
     * @since 1.0.0
     */
    override fun pathPrefix(): String =
        requireNotNull(properties.publicEndpoint) { "publicEndpoint is null" }

    /** 日志器 */
    private val LOG = LogFactory.getLog(this::class)

}
