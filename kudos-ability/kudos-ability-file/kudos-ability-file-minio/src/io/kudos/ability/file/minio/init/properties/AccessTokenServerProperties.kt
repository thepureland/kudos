package io.kudos.ability.file.minio.init.properties

/**
 * MinIO STS (Security Token Service) 简化版的 OAuth2 客户端配置。
 *
 * 对应 yml 路径 `kudos.ability.file.minio.sts.access-token.*`。业务侧 token 通过
 * [io.kudos.ability.file.common.auth.AccessTokenServerParam.headerValue] 透传，
 * 框架拿 token 去本类配置的 OAuth2 端点换 MinIO 凭证。
 *
 * @property enabled 是否启用 OAuth2 STS 模式
 * @property clientId OAuth2 客户端 id（写到 Basic Authorization header）
 * @property clientSecret OAuth2 客户端密码（同上；**不要落到日志**）
 * @property authorizationGrantType OAuth2 授权类型，典型 `client_credentials`
 * @property clientAuthenticationMethod 客户端认证方式（保留字段，当前实现总是用 Basic）
 * @property endpoint OAuth2 token 端点的完整 URL（POST 到这里换 JWT）
 * @property headerName 把业务侧的认证 token 透传给 OAuth2 服务器时使用的 header 名
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class AccessTokenServerProperties : AuthServerProperties() {
    /** 是否启用 OAuth2 STS 模式；null 视为禁用 */
    var enabled: Boolean? = null

    /** OAuth2 客户端 id（用于 Basic Authorization 头） */
    var clientId: String? = null

    /** OAuth2 客户端密码；**不要写入日志** */
    var clientSecret: String? = null

    /** OAuth2 授权类型，典型 `client_credentials` */
    var authorizationGrantType: String? = null

    /** 客户端认证方式（保留字段，当前实现总是用 Basic Authorization） */
    var clientAuthenticationMethod: String? = null

    /**
     * 获取Minio STS所需 JWT数据的端点
     */
    var endpoint: String? = null

    /**
     * 已经认证Token的请求头header名
     */
    var headerName: String? = null
}
