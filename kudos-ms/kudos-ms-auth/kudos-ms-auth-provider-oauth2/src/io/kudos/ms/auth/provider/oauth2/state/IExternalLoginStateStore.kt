package io.kudos.ms.auth.provider.oauth2.state

interface IExternalLoginStateStore {
    fun create(state: ExternalLoginState): Boolean

    /** Atomically returns and deletes a state; a second callback must receive null. */
    fun consume(state: String): ExternalLoginState?
}
