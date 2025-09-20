package io.kudos.ability.file.minio.init.properties

class AccessTokenServerProperties : AuthServerProperties() {
    var enabled: Boolean? = null

    var clientId: String? = null

    var clientSecret: String? = null

    var authorizationGrantType: String? = null

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
