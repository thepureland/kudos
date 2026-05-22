package io.kudos.ability.file.minio.client

import io.kudos.ability.file.common.auth.AccessKeyServerParam
import io.kudos.ability.file.minio.init.properties.MinioProperties
import io.minio.MinioClient
import java.net.URI

/**
 * 用 AK/SK 静态凭证构建 [MinioClient]。
 *
 * 适用场景：服务自身持有的固定 access key + secret key（例如配置文件、KMS 取出的长期凭证）。
 * 与 [AccessTokenMinioClientBuilder] 的区别是后者用 OAuth2 token 走 STS 临时凭证，租期更短。
 *
 * @author Roger
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class AccessKeyMinioClientBuilder : MinioClientBuilder<AccessKeyServerParam> {

    /** MinIO 全局配置（提供 endpoint） */
    private var minioProperties: MinioProperties? = null

    /** 当前次请求所用的 AK/SK */
    private var authServerParam: AccessKeyServerParam? = null

    /**
     * 用 endpoint + AK/SK 组装 [MinioClient] 并返回。
     *
     * @return 全新的 MinIO 客户端实例
     * @throws IllegalArgumentException 配置缺失时
     * @author K
     * @since 1.0.0
     */
    override fun build(): MinioClient {
        val props = requireNotNull(minioProperties) { "minioProperties is null" }
        val auth = requireNotNull(authServerParam) { "authServerParam is null" }
        return MinioClient.builder()
            .endpoint(URI(props.endpoint).toURL())
            .credentials(auth.accessKey, auth.secretKey)
            .build()
    }

    /**
     * 注入 MinIO 全局配置。
     *
     * @param minioProperties 配置对象
     * @author K
     * @since 1.0.0
     */
    fun setMinioProperties(minioProperties: MinioProperties) {
        this.minioProperties = minioProperties
    }

    /**
     * 注入本次请求所用的 AK/SK 凭证。
     *
     * @param authServerParam 鉴权参数
     * @author K
     * @since 1.0.0
     */
    override fun setAuthServerParam(authServerParam: AccessKeyServerParam) {
        this.authServerParam = authServerParam
    }

}
