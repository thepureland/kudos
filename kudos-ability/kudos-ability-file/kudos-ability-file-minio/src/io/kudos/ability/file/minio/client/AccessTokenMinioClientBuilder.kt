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
 * Minio request authentication (simplified STS): calls the configured OAuth2 token endpoint to obtain an
 * access_token, wraps it as a [Jwt] and feeds it to Minio's [WebIdentityProvider] - the business side only
 * needs to pass through an existing authentication token via [AccessTokenServerParam.headerValue].
 *
 * @author Roger
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class AccessTokenMinioClientBuilder : MinioClientBuilder<AccessTokenServerParam> {

    /** Logger, used when debugging OAuth2 token endpoint interactions. */
    private val log = LogFactory.getLog(this::class)

    /** Global MinIO configuration (endpoint). */
    private lateinit var minioProperties: MinioProperties

    /** OAuth2 token endpoint configuration (authorization grant type / clientId / clientSecret / endpoint / headerName). */
    private lateinit var accessTokenServerProperties: AccessTokenServerProperties

    /** Token carried in the current request (passed through from the business layer). */
    private var authServerParam: AccessTokenServerParam? = null

    /**
     * Injects the token parameter for the current request.
     *
     * @param authServerParam token authentication parameters
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
     * Calls the OAuth2 token endpoint to obtain a [Jwt].
     *
     * Request headers: business-side passed-through `headerValue` (e.g., the user's access token) +
     * Basic clientId:clientSecret. Security note: the old implementation used to `log.info` the
     * `jwt.token()` string here - a replayable access_token landing in aggregated logs is a real leak
     * surface, now changed to DEBUG and only the expiry seconds, not the token itself.
     *
     * @param authServerParam token authentication parameters passed through from the business side
     * @return the obtained [Jwt]; throws [ProviderException] on network failure
     * @throws ProviderException thrown after wrapping an IO exception
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
                // Historical issue: the old implementation used to log.info jwt.token() here -
                // writing a replayable access_token into process logs / log aggregation is a real
                // leak surface. Here we only debug-log the expiry, not the token string itself.
                jwt?.let { log.debug("Minio oauth2 server token acquired, expires_in={0}s", it.expiry()) }
                return jwt
            }
        } catch (e: IOException) {
            throw ProviderException(e)
        }
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
     * Injects the OAuth2 token endpoint configuration.
     *
     * @param accessTokenServerProperties token endpoint configuration
     * @author K
     * @since 1.0.0
     */
    fun setAccessTokenServerProperties(accessTokenServerProperties: AccessTokenServerProperties) {
        this.accessTokenServerProperties = accessTokenServerProperties
    }

    companion object {
        /**
         * Shared OkHttp client singleton. The old implementation instantiated `OkHttpClient()` on every
         * `accessToken()` call - a heavy object (thread pool + connection pool + dispatcher); instantiating
         * one per request is a significant resource waste.
         */
        private val httpClient = OkHttpClient()

        /**
         * Shared Jackson 3 mapper singleton. `changeDefaultVisibility { withFieldVisibility(ANY) }` is
         * required to deserialize the private final fields of Minio [Jwt] (no getter / no no-arg
         * constructor). See [io.kudos.ability.file.minio.client.AccessTokenJwtMapperTest] for details.
         */
        private val mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .changeDefaultVisibility { it.withFieldVisibility(JsonAutoDetect.Visibility.ANY) }
            .build()
    }

}
