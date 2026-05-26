package io.kudos.ability.file.minio.client

import io.kudos.ability.file.common.auth.AccessKeyServerParam
import io.kudos.ability.file.common.auth.AccessTokenServerParam
import io.kudos.ability.file.common.auth.AuthServerParam
import io.kudos.ability.file.minio.init.properties.AccessTokenServerProperties
import io.kudos.ability.file.minio.init.properties.MinioProperties
import org.springframework.beans.factory.annotation.Autowired


/**
 * Dispatches to the corresponding [MinioClientBuilder] implementation based on the [AuthServerParam] subtype.
 *
 * Known subtype -> builder mapping:
 *  - [AccessKeyServerParam] -> [AccessKeyMinioClientBuilder] (AK/SK direct connection)
 *  - [AccessTokenServerParam] -> [AccessTokenMinioClientBuilder] (OAuth2 token -> MinIO STS)
 *  - Unknown type -> null (the caller decides whether to fall back to a default client / throw an error)
 *
 * When the business side adds a new authentication form: (a) add an `AuthServerParam` subclass in file-common,
 * (b) add the corresponding `MinioClientBuilder` implementation in file-minio, (c) add a branch to the `when`
 * in this factory.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class MinioClientBuilderFactory {

    @Autowired
    private lateinit var minioProperties: MinioProperties

    @Autowired
    private lateinit var accessTokenServer: AccessTokenServerProperties

    /** Returns a fully-initialized builder based on the actual type of [authServerParam]; returns null for unknown types. */
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
