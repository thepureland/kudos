package io.kudos.ms.user.common.security

/** Trusted authorization backend port; role names supplied by a caller are never sufficient. */
fun interface IPlatformAdministratorPolicy {
    fun isPlatformAdministrator(principalId: String): Boolean
}
