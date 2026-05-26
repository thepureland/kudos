package io.kudos.ability.data.rdb.jdbc.aop

/**
 * Method-level data-source-switching annotation for multi-tenant scenarios, intercepted by
 * [TenantDsChangeAspect].
 *
 * Differs from [DsChange]: [value] is not used directly as a data source key; instead it is
 * treated as a "service code", wrapped by the aspect into the form `_context::<value>` and
 * written into `DbParam.forcedDs`, after which `DynamicDataSourceAspect` resolves the actual
 * data source key dynamically based on the current tenant + service code.
 *
 * Use case: in multi-tenant apps, semantics like "this business method should hit the data source
 * belonging to the current tenant under service X" — the annotation expresses intent, and the
 * aspect performs the routing lookup.
 *
 * @property value service code; an empty string performs no switch. Strings already prefixed with
 *   `_context` are forwarded as-is.
 * @property readonly true means route to a read-only replica.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class TenantDsChange(val value: String = "", val readonly: Boolean = false)
