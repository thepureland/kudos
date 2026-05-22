package io.kudos.ability.file.minio.client

import io.kudos.ability.file.common.auth.AccessKeyServerParam
import io.kudos.ability.file.common.auth.AccessTokenServerParam
import io.kudos.ability.file.common.auth.AuthServerParam
import io.kudos.ability.file.minio.init.properties.AccessTokenServerProperties
import io.kudos.ability.file.minio.init.properties.MinioProperties
import org.springframework.beans.factory.annotation.Autowired


/**
 * 按 [AuthServerParam] 子类型分发到对应的 [MinioClientBuilder] 实现。
 *
 * 已知子类型 → builder 映射：
 *  - [AccessKeyServerParam] → [AccessKeyMinioClientBuilder]（AK/SK 直连）
 *  - [AccessTokenServerParam] → [AccessTokenMinioClientBuilder]（OAuth2 token → MinIO STS）
 *  - 未知类型 → null（调用方需自行决定回退到默认客户端 / 抛错）
 *
 * 业务侧扩展新认证形式时：(a) 在 file-common 增加 `AuthServerParam` 子类，
 * (b) 在 file-minio 增加对应 `MinioClientBuilder` 实现，(c) 在本工厂的 when 增加分支。
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

    /** 按 [authServerParam] 实际类型返回组装好属性的 builder；未知类型返回 null。 */
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
