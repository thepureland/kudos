package io.kudos.ability.file.minio.init.properties

/**
 * OAuth2 client configuration for a simplified MinIO STS (Security Token Service).
 *
 * Corresponds to the yml path `kudos.ability.file.minio.sts.access-token.*`. The
 * business token is passed through via
 * [io.kudos.ability.file.common.auth.AccessTokenServerParam.headerValue]; the
 * framework uses that token to exchange for MinIO credentials at the OAuth2
 * endpoint configured here.
 *
 * @property enabled whether the OAuth2 STS mode is enabled
 * @property clientId OAuth2 client id (written into the Basic Authorization header)
 * @property clientSecret OAuth2 client secret (same as above; **must not be logged**)
 * @property authorizationGrantType OAuth2 grant type, typically `client_credentials`
 * @property clientAuthenticationMethod client authentication method (reserved; the current implementation always uses Basic)
 * @property endpoint full URL of the OAuth2 token endpoint (POST here to exchange for a JWT)
 * @property headerName header name used when forwarding the business-side authentication token to the OAuth2 server
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class AccessTokenServerProperties : AuthServerProperties() {
    /** Whether the OAuth2 STS mode is enabled; null is treated as disabled. */
    var enabled: Boolean? = null

    /** OAuth2 client id (used in the Basic Authorization header). */
    var clientId: String? = null

    /** OAuth2 client secret; **must not be written to logs**. */
    var clientSecret: String? = null

    /** OAuth2 grant type, typically `client_credentials`. */
    var authorizationGrantType: String? = null

    /** Client authentication method (reserved; the current implementation always uses Basic Authorization). */
    var clientAuthenticationMethod: String? = null

    /**
     * Endpoint for obtaining the JWT data required by the MinIO STS.
     */
    var endpoint: String? = null

    /**
     * Name of the request header carrying the authenticated token.
     */
    var headerName: String? = null
}
