package io.kudos.ability.file.minio.client

import com.fasterxml.jackson.annotation.JsonAutoDetect
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import io.kudos.ability.file.common.auth.AccessTokenServerParam
import io.kudos.ability.file.minio.init.properties.AccessTokenServerProperties
import io.kudos.ability.file.minio.init.properties.MinioProperties
import io.kudos.base.lang.string.EncodeKit
import io.kudos.base.logger.LogFactory
import io.minio.MinioClient
import io.minio.credentials.Jwt
import io.minio.credentials.Provider
import io.minio.credentials.WebIdentityProvider
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.ProviderException

/**
 * Minio 请求认证（STS 简化版）：调用配置的 OAuth2 token 端点拿 access_token，
 * 包装成 [Jwt] 喂给 Minio 的 [WebIdentityProvider] —— 业务侧只需把已有的认证 Token 通过
 * [AccessTokenServerParam.headerValue] 透传过来。
 *
 * @author Roger
 * @author K
 * @since 1.0.0
 */
open class AccessTokenMinioClientBuilder : MinioClientBuilder<AccessTokenServerParam> {

    /** 日志器，调试 OAuth2 token 端点交互时使用 */
    private val log = LogFactory.getLog(this::class)

    /** MinIO 全局配置（endpoint） */
    private lateinit var minioProperties: MinioProperties

    /** OAuth2 token 端点配置（authorization grant type / clientId / clientSecret / endpoint / headerName） */
    private lateinit var accessTokenServerProperties: AccessTokenServerProperties

    /** 当前次请求携带的 token（业务上层透传） */
    private var authServerParam: AccessTokenServerParam? = null

    /**
     * 注入本次请求的 token 参数。
     *
     * @param authServerParam token 鉴权参数
     * @author K
     * @since 1.0.0
     */
    override fun setAuthServerParam(authServerParam: AccessTokenServerParam) {
        this.authServerParam = authServerParam
    }

    @Throws(Exception::class)
    override fun build(): MinioClient {
        val provider: Provider = WebIdentityProvider(
            { accessToken(requireNotNull(authServerParam) { "authServerParam is null" }) },
            requireNotNull(minioProperties.endpoint) { "endpoint is null" },
            null,
            null,
            null,
            null,
            null
        )

        return MinioClient.builder()
            .endpoint(minioProperties.endpoint)
            .credentialsProvider(provider)
            .build()
    }

    /**
     * 调 OAuth2 token 端点拿 [Jwt]。
     *
     * 请求头：业务方透传的 `headerValue`（如用户的 access token）+ Basic clientId:clientSecret。
     * 安全注意：旧实现曾在此 `log.info` 输出 `jwt.token()` 字符串——可重放的 access_token 落到聚合日志里
     * 就是一个泄漏面，已改为 DEBUG 仅打过期秒数，不打 token 本身。
     *
     * @param authServerParam 业务侧透传的 token 鉴权参数
     * @return 拿到的 [Jwt]；网络失败时抛 [ProviderException]
     * @throws ProviderException IO 异常包装后抛出
     * @author K
     * @since 1.0.0
     */
    protected fun accessToken(authServerParam: AccessTokenServerParam): Jwt? {
        val requestBody: RequestBody = FormBody.Builder()
            .add("grant_type", requireNotNull(accessTokenServerProperties.authorizationGrantType) { "authorizationGrantType is null" })
            .build()

        val basicRaw = "${accessTokenServerProperties.clientId}:${accessTokenServerProperties.clientSecret}"
        val basic = EncodeKit.encodeBase64(basicRaw.toByteArray(StandardCharsets.UTF_8))
        log.info("Minio oauth2 server: ${accessTokenServerProperties.endpoint}")
        val request = Request.Builder()
            .url(requireNotNull(accessTokenServerProperties.endpoint) { "endpoint is null" })
            .header(requireNotNull(accessTokenServerProperties.headerName) { "headerName is null" }, requireNotNull(authServerParam.headerValue) { "headerValue is null" })
            .header("Authorization", "Basic $basic")
            .post(requestBody).build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val rs = String(response.body.bytes())
                val jwt = mapper.readValue(rs, Jwt::class.java)
                // 历史问题：旧实现在这里 log.info 输出 jwt.token() —— 把可重放的 access_token
                // 写进进程日志 / 日志聚合，是真实泄漏面。这里只 debug 记录 expiry 不打 token 字符串。
                if (jwt != null) {
                    log.debug("Minio oauth2 server token acquired, expires_in={0}s", jwt.expiry())
                }
                return jwt
            }
        } catch (e: IOException) {
            throw ProviderException(e)
        }
    }

    /**
     * 注入 MinIO 全局配置。
     *
     * @param minioProperties 配置对象
     * @author K
     * @since 1.0.0
     */
    fun setMinioProperties(minioProperties: MinioProperties) {
        this.minioProperties = minioProperties
    }

    /**
     * 注入 OAuth2 token 端点配置。
     *
     * @param accessTokenServerProperties token 端点配置
     * @author K
     * @since 1.0.0
     */
    fun setAccessTokenServerProperties(accessTokenServerProperties: AccessTokenServerProperties) {
        this.accessTokenServerProperties = accessTokenServerProperties
    }

    companion object {
        /**
         * OkHttp 客户端共享单例。旧实现每次 `accessToken()` 调用都 `OkHttpClient()` 新建 —— 重型对象
         * （线程池 + 连接池 + dispatcher），每请求一次实例化是显著的资源浪费。
         */
        private val httpClient = OkHttpClient()

        /**
         * Jackson 3 mapper 共享单例。`changeDefaultVisibility { withFieldVisibility(ANY) }` 是为了
         * 反序列化 Minio [Jwt] 的私有 final 字段（无 getter / 无无参构造）。详见
         * [io.kudos.ability.file.minio.client.AccessTokenJwtMapperTest]。
         */
        private val mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .changeDefaultVisibility { it.withFieldVisibility(JsonAutoDetect.Visibility.ANY) }
            .build()
    }

}
