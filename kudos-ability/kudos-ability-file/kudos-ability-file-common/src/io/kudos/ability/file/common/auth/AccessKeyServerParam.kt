package io.kudos.ability.file.common.auth

/**
 * Username/password-based authentication parameters.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class AccessKeyServerParam : AuthServerParam {
    /**
     * Username.
     */
    var accessKey: String? = null

    /**
     * Password.
     */
    var secretKey: String? = null

    constructor()

    constructor(accessKey: String?, secretKey: String?) {
        this.accessKey = accessKey
        this.secretKey = secretKey
    }
}
