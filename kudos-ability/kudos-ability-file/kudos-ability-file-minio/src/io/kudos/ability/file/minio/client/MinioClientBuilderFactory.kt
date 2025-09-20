package io.kudos.ability.file.minio.client

import io.kudos.ability.file.common.auth.AccessKeyServerParam
import io.kudos.ability.file.common.auth.AccessTokenServerParam
import io.kudos.ability.file.common.auth.AuthServerParam
import io.kudos.ability.file.minio.init.properties.AccessTokenServerProperties
import io.kudos.ability.file.minio.init.properties.MinioProperties
import org.springframework.beans.factory.annotation.Autowired


class MinioClientBuilderFactory {

    @Autowired
    private lateinit var minioProperties: MinioProperties

    @Autowired
    private lateinit var accessTokenServer: AccessTokenServerProperties

    fun getInstance(authServerParam: AuthServerParam): MinioClientBuilder<*>? {
        if (authServerParam is AccessKeyServerParam) {
            val builder = AccessKeyMinioClientBuilder()
            builder.setMinioProperties(minioProperties)
            builder.setAuthServerParam(authServerParam)
            return builder
        }
        if (authServerParam is AccessTokenServerParam) {
            val builder = AccessTokenMinioClientBuilder()
            builder.setMinioProperties(minioProperties)
            builder.setAccessTokenServerProperties(accessTokenServer)
            builder.setAuthServerParam(authServerParam)
            return builder
        }
        return null
    }

}
