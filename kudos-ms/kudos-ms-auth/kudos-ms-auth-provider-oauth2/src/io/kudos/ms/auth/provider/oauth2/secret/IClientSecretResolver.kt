package io.kudos.ms.auth.provider.oauth2.secret

/** Resolves a secret reference without exposing secret values through the provider catalog. */
interface IClientSecretResolver {
    fun supports(reference: String): Boolean
    fun resolve(reference: String): String?

    /** Clears resolver-owned local state after an external secret is rotated at the same reference. */
    fun invalidate(reference: String) = Unit
}
