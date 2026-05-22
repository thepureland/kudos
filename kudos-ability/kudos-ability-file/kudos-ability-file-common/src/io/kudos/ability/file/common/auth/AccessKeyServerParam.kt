package io.kudos.ability.file.common.auth

/**
 * 基于用户名 密码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class AccessKeyServerParam : AuthServerParam {
    /**
     * 用户名
     */
    var accessKey: String? = null

    /**
     * 密码
     */
    var secretKey: String? = null

    constructor()

    constructor(accessKey: String?, secretKey: String?) {
        this.accessKey = accessKey
        this.secretKey = secretKey
    }
}
