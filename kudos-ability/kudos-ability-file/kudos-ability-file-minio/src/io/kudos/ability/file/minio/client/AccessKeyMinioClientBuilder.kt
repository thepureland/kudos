package io.kudos.ability.file.minio.client

import io.kudos.ability.file.common.auth.AccessKeyServerParam
import io.kudos.ability.file.minio.init.properties.MinioProperties
import io.minio.MinioClient
import java.net.URI

/**
 * Minio 请求认证,
 * 1) 通过向资源中心请求用户名 + 密码
 *
 * @author Roger
 */
class AccessKeyMinioClientBuilder : MinioClientBuilder<AccessKeyServerParam> {

    private var minioProperties: MinioProperties? = null

    private var authServerParam: AccessKeyServerParam? = null

    override fun build(): MinioClient {
        return MinioClient.builder()
            .endpoint(URI(requireNotNull(minioProperties) { "minioProperties is null" }.endpoint).toURL())
            .credentials(requireNotNull(authServerParam) { "authServerParam is null" }.accessKey, requireNotNull(authServerParam) { "authServerParam is null" }.secretKey)
            .build()
    }

    fun setMinioProperties(minioProperties: MinioProperties) {
        this.minioProperties = minioProperties
    }

    override fun setAuthServerParam(authServerParam: AccessKeyServerParam) {
        this.authServerParam = authServerParam
    }

}
