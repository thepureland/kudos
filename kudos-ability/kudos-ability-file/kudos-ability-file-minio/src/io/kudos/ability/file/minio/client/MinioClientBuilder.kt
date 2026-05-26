package io.kudos.ability.file.minio.client

import io.kudos.ability.file.common.auth.AuthServerParam
import io.minio.MinioClient


/**
 * Minio STS client acquisition interface.
 * The [MinioClient] obtained through this interface is authorized with certain permission controls.
 *
 * @param T authentication parameter type
 * @author Roger
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 * @see [Minio STS](https://min.io/docs/minio/linux/developers/security-token-service.html)
 */
interface MinioClientBuilder<T : AuthServerParam> {
    fun setAuthServerParam(authServerParam: T)

    /**
     * Obtains a [MinioClient] with permissions, using resource server (e.g., OpenId server) parameters.
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    fun build(): MinioClient
}
