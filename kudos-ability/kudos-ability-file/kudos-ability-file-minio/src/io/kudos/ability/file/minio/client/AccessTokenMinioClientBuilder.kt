package io.kudos.ability.file.minio.client

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.introspect.VisibilityChecker
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
import java.io.UnsupportedEncodingException
import java.security.ProviderException

/**
 * Minio 请求认证
 * STS 简化版, 直接获取资源中心的AccessToken
 * 1) 通过向资源中心请求已经认证Token 免去资源中心用户认证
 *
 * @author Roger
 */
open class AccessTokenMinioClientBuilder : MinioClientBuilder<AccessTokenServerParam> {

    private val log = LogFactory.getLog(this)

    private lateinit var minioProperties: MinioProperties

    private lateinit var accessTokenServerProperties: AccessTokenServerProperties

    private var authServerParam: AccessTokenServerParam? = null
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

    protected fun accessToken(authServerParam: AccessTokenServerParam): Jwt? {
        val requestBody: RequestBody = FormBody.Builder()
            .add("grant_type", requireNotNull(accessTokenServerProperties.authorizationGrantType) { "authorizationGrantType is null" })
            .build()

        var basic = "${accessTokenServerProperties.clientId}:${accessTokenServerProperties.clientSecret}"
        try {
            basic = EncodeKit.encodeBase64(basic.toByteArray(charset("UTF-8")))
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
        log.info("Minio oauth2 server: ${accessTokenServerProperties.endpoint}")
        val request = Request.Builder()
            .url(requireNotNull(accessTokenServerProperties.endpoint) { "endpoint is null" })
            .header(requireNotNull(accessTokenServerProperties.headerName) { "headerName is null" }, requireNotNull(authServerParam.headerValue) { "headerValue is null" })
            .header("Authorization", "Basic $basic")
            .post(requestBody).build()

        val client = OkHttpClient()
        try {
            client.newCall(request).execute().use { response ->
                val mapper = ObjectMapper()
                mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                mapper.setVisibility(
                    VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                )
                val rs = String(response.body.bytes())
                val jwt = mapper.readValue(rs, Jwt::class.java)
                if (jwt != null) {
                    log.info("Minio oauth2 server OIDC token:${jwt.token()}")
                }
                return jwt
            }
        } catch (e: IOException) {
            throw ProviderException(e)
        }
    }

    fun setMinioProperties(minioProperties: MinioProperties) {
        this.minioProperties = minioProperties
    }

    fun setAccessTokenServerProperties(accessTokenServerProperties: AccessTokenServerProperties) {
        this.accessTokenServerProperties = accessTokenServerProperties
    }

}
