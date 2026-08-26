package io.kudos.ms.auth.provider.webauthn.ceremony

interface IWebAuthnCeremonyStore {
    fun create(state: WebAuthnCeremonyState): Boolean
    fun get(id: String): WebAuthnCeremonyState?
    fun consume(id: String): WebAuthnCeremonyState?
}
