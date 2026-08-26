package io.kudos.ms.auth.core.authentication.assurance.spi

/** Compares protocol-neutral authentication context class references (ACR). */
fun interface IAuthenticationAssurancePolicy {

    /** Returns true only when [actualAcr] is at least as strong as [requiredAcr]. */
    fun isSatisfied(actualAcr: String, requiredAcr: String): Boolean
}
