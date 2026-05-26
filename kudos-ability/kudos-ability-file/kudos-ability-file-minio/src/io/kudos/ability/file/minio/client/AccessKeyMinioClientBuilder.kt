package io.kudos.ability.file.minio.client

import io.kudos.ability.file.common.auth.AccessKeyServerParam
import io.kudos.ability.file.minio.init.properties.MinioProperties
import io.minio.MinioClient
import java.net.URI

/**
 * Builds [MinioClient] using static AK/SK credentials.
 *
 * Applicable scenarios: fixed access key + secret key held by the service itself (e.g., long-term credentials
 * from configuration files or KMS). The difference from [AccessTokenMinioClientBuilder] is that the latter
 * uses an OAuth2 token via STS temporary credentials with a shorter lease.
 *
 * @author Roger
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class AccessKeyMinioClientBuilder : MinioClientBuilder<AccessKeyServerParam> {

    /** Global MinIO configuration (provides endpoint). */
    private var minioProperties: MinioProperties? = null

    /** AK/SK used for the current request. */
    private var authServerParam: AccessKeyServerParam? = null

    /**
     * Assembles a [MinioClient] using endpoint + AK/SK and returns it.
     *
     * @return a brand-new MinIO client instance
     * @throws IllegalArgumentException when configuration is missing
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
     * Injects the global MinIO configuration.
     *
     * @param minioProperties configuration object
     * @author K
     * @since 1.0.0
     */
    fun setMinioProperties(minioProperties: MinioProperties) {
        this.minioProperties = minioProperties
    }

    /**
     * Injects the AK/SK credentials used for the current request.
     *
     * @param authServerParam authentication parameters
     * @author K
     * @since 1.0.0
     */
    override fun setAuthServerParam(authServerParam: AccessKeyServerParam) {
        this.authServerParam = authServerParam
    }

}
