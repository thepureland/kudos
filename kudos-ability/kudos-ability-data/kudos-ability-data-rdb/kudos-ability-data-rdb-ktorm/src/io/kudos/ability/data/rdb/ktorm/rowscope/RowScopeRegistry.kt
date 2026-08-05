package io.kudos.ability.data.rdb.ktorm.rowscope

import io.kudos.base.logger.LogFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass


/**
 * Resolves an entity class to its row-scope declaration, from annotations and from any contributed
 * [IRowScopePolicyProvider].
 *
 * Results are memoised: the declaration is a property of the class, so reflecting on every query
 * would be pure waste on the hottest path in the framework.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class RowScopeRegistry(
    providers: List<IRowScopePolicyProvider> = emptyList(),
) {

    private val log = LogFactory.getLog(this::class)

    /** Declarations contributed for entities whose source the deployment does not own. */
    private val contributed: Map<KClass<*>, RowScopePolicy> =
        providers.flatMap { it.policies() }.associateBy { it.entityClass }

    private val cache = ConcurrentHashMap<KClass<*>, RowScopePolicy>()

    /**
     * The policy for [entityClass]; never null — an entity that declares nothing gets a policy that
     * [RowScopePolicy.participates] reports false for, so callers have one shape to handle.
     */
    open fun policyOf(entityClass: KClass<*>): RowScopePolicy =
        cache.getOrPut(entityClass) { contributed[entityClass] ?: fromAnnotations(entityClass) }

    /** Every entity the deployment has explicitly declared. Used by the startup report. */
    open fun declaredEntities(): Set<KClass<*>> = contributed.keys + cache.filterValues { it.participates }.keys

    private fun fromAnnotations(entityClass: KClass<*>): RowScopePolicy {
        val java = entityClass.java
        val dimensions = java.getAnnotationsByType(DataScoped::class.java)
            .associate { it.dimension.trim() to it.column.trim() }
            .filterKeys { it.isNotEmpty() }
            .filterValues { it.isNotEmpty() }
        val selfColumn = java.getAnnotation(DataScopedSelf::class.java)?.column?.trim()?.takeIf { it.isNotEmpty() }
        val policy = RowScopePolicy(entityClass, dimensions, selfColumn)
        if (policy.participates) {
            log.debug("Row scope for ${entityClass.simpleName}: dimensions=${dimensions.keys}, self=${selfColumn ?: "-"}.")
        }
        return policy
    }

    /**
     * Warns about entities that declare some dimensions but not others the deployment registered.
     *
     * A partial declaration is worse than none: the entity looks protected on the screen where the
     * scope was configured, and is not, on whichever dimension was forgotten.
     *
     * @param registeredDimensions every dimension the authorization side knows about
     * @return the human-readable findings, empty when nothing looks partial
     */
    open fun auditPartialDeclarations(registeredDimensions: Set<String>): List<String> {
        if (registeredDimensions.isEmpty()) return emptyList()
        return cache.values
            .filter { it.dimensionColumns.isNotEmpty() }
            .mapNotNull { policy ->
                val missing = registeredDimensions - policy.dimensionColumns.keys
                if (missing.isEmpty()) null
                else "${policy.entityClass.simpleName} declares ${policy.dimensionColumns.keys} but not ${missing}"
            }
    }
}
