package io.kudos.ability.file.minio.client

import io.kudos.ability.file.common.auth.AuthServerParam
import io.minio.MinioClient


/**
 * Minio STS 客户端获取接口
 * 通过此接口获取的 [MinioClient] 授权一定的权限控制
 *
 * @param <T>
 * @author Roger
 * @see [Minio STS](https://min.io/docs/minio/linux/developers/security-token-service.html)
</T> */
interface MinioClientBuilder<T : AuthServerParam> {
    fun setAuthServerParam(authServerParam: T)

    /**
     * 通过资源服务器(如:OpenId服务器)参数,获取带权限的 [MinioClient]
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    fun build(): MinioClient
}
