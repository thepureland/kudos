package io.kudos.ms.auth.core.role.source

import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


/**
 * Collects the roles every registered [IRoleSourceProvider] contributes for a principal.
 *
 * One place asks, so the built-in resolution paths — the effective role set and the decision point's
 * grant list — stay in agreement no matter how many sources a deployment adds.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class RoleSourceRegistry {

    /**
     * Injected as an optional list so the set of sources is open *and* may be empty — which is the
     * stock case, where [contributedRoleIds] short-circuits and every resolution path behaves
     * exactly as it did before this SPI existed. `lateinit` would refuse to start a deployment that
     * registered none.
     */
    @Autowired(required = false)
    private var providers: List<IRoleSourceProvider> = emptyList()

    private val log = LogFactory.getLog(this::class)

    /**
     * Every role id contributed for [principalId], across all sources.
     *
     * A provider that throws is skipped rather than allowed to fail the whole resolution: an
     * external system being down must not turn into "this person has no permissions at all", which
     * would be an outage manufactured out of a dependency's outage. It does mean the contributed
     * roles are absent until it recovers — logged at warn, and strictly narrower than the truth,
     * which is the safe direction.
     *
     * @param principalId the principal
     * @return the contributed role ids, deduplicated
     */
    open fun contributedRoleIds(principalId: String): Set<String> {
        if (providers.isEmpty()) return emptySet()
        val contributed = LinkedHashSet<String>()
        providers.forEach { provider ->
            runCatching { provider.roleIdsOf(principalId) }
                .onSuccess { contributed.addAll(it) }
                .onFailure {
                    log.warn(
                        "Role source '${provider.sourceName()}' failed for principal ${principalId}; " +
                            "its roles are omitted from this resolution.",
                        it,
                    )
                }
        }
        return contributed
    }

    /** True when the deployment registered no external source at all — the stock case. */
    open fun isEmpty(): Boolean = providers.isEmpty()

    /** Registered source names, for explanations and diagnostics. */
    open fun sourceNames(): List<String> = providers.map { it.sourceName() }
}
