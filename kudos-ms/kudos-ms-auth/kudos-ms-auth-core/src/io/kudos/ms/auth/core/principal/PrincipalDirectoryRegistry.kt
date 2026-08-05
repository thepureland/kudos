package io.kudos.ms.auth.core.principal

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.principal.spi.IPrincipalDirectory
import io.kudos.ms.auth.core.principal.spi.PrincipalFacts
import io.kudos.ms.auth.core.principal.spi.UserAccountPrincipalDirectory.Companion.PRINCIPAL_TYPE_USER
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * Looks a principal up across every registered [IPrincipalDirectory].
 *
 * One place asks "who is this", so adding a principal kind is a matter of contributing a bean and
 * nothing in the policy or decision layers changes.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class PrincipalDirectoryRegistry {

    /**
     * Injected as a list so the set of principal kinds is open. Ordering is irrelevant: a provider
     * answers only for its own kind, and lookups by id fall back across kinds in a defined way (see
     * [find]).
     */
    @Resource
    private lateinit var directories: List<IPrincipalDirectory>

    private val log = LogFactory.getLog(this::class)

    /**
     * The principal's facts.
     *
     * When [principalType] is given, only the provider claiming that kind is asked — a caller that
     * knows what it is holding should not be silently answered by a different directory.
     *
     * When it is null (the common case: a write path that only ever carried an id), every provider
     * is asked and the first hit wins. That is sound because principal ids are unique across kinds;
     * see the invariant on [IPrincipalDirectory]. A second hit means that invariant is broken, which
     * is worth a warning rather than an arbitrary pick.
     *
     * @param principalId the principal
     * @param principalType its kind, when the caller knows it
     * @return the facts, or null when nobody claims this principal
     */
    open fun find(principalId: String, principalType: String? = null): PrincipalFacts? {
        if (principalId.isBlank()) return null
        if (principalType != null) {
            return directories.firstOrNull { it.principalType().equals(principalType, ignoreCase = true) }
                ?.find(principalId)
        }
        val hits = directories.mapNotNull { it.find(principalId) }
        if (hits.size > 1) {
            log.warn(
                "Principal ${principalId} was claimed by ${hits.size} directories " +
                    "(${hits.joinToString { it.principalType }}); principal ids must be unique across " +
                    "kinds. Using ${hits.first().principalType}.",
            )
        }
        return hits.firstOrNull()
    }

    /** Every registered principal kind, for diagnostics and for the admin UI. */
    open fun registeredTypes(): List<String> =
        directories.map { it.principalType() }.distinct().sorted()

    /** True when the deployment has registered nothing beyond the built-in human accounts. */
    open fun onlyHumanPrincipals(): Boolean = registeredTypes() == listOf(PRINCIPAL_TYPE_USER)
}
