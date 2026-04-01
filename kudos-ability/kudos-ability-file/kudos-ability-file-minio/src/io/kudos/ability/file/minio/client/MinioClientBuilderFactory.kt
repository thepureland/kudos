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

    fun getInstance(authServerParam: AuthServerParam): MinioClientBuilder<*>? = when (authServerParam) {
        is AccessKeyServerParam -> AccessKeyMinioClientBuilder().apply {
            setMinioProperties(minioProperties)
            setAuthServerParam(authServerParam)
        }
        is AccessTokenServerParam -> AccessTokenMinioClientBuilder().apply {
            setMinioProperties(minioProperties)
            setAccessTokenServerProperties(accessTokenServer)
            setAuthServerParam(authServerParam)
        }
        else -> null
    }

}
