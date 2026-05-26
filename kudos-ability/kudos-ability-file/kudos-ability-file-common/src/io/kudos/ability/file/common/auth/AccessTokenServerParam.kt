package io.kudos.ability.file.common.auth

/**
 * AccessToken (HTTP-header-injection style) authentication parameter: puts
 * `headerValue` directly into the Authorization header of each upload request.
 *
 * Used in scenarios such as [io.kudos.ability.file.minio.MinioUploadService] for
 * "dynamic authentication" — the caller injects a token per tenant/session,
 * distinct from the static AK/SK mode preconfigured in the configuration file.
 *
 * @property headerValue full value of the Authorization header (the caller is
 *                       responsible for formatting, including any `Bearer ` prefix)
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class AccessTokenServerParam : AuthServerParam {
    var headerValue: String? = null
}
