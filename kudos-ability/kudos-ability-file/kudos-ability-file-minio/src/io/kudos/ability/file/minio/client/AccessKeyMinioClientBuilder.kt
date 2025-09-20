package io.kudos.ability.file.minio.client

import io.kudos.ability.file.common.auth.AccessKeyServerParam
import io.kudos.ability.file.minio.init.properties.MinioProperties
import io.kudos.base.logger.LogFactory
import io.minio.MinioClient
import java.net.URL

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
            .endpoint(URL(minioProperties!!.endpoint))
            .credentials(authServerParam!!.accessKey, authServerParam!!.secretKey)
            .build()
    }

    fun setMinioProperties(minioProperties: MinioProperties) {
        this.minioProperties = minioProperties
    }

    override fun setAuthServerParam(authServerParam: AccessKeyServerParam) {
        this.authServerParam = authServerParam
    }

    private val LOG = LogFactory.getLog(this)

}
