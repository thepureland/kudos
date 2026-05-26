package io.kudos.ability.data.rdb.jdbc.aop

/**
 * Method-level annotation that forces a data source switch. Intercepted by [DsChangeAspect]:
 * before method execution it writes [value] into the current thread's `DbParam.forcedDs` and
 * [readonly] into `DbParam.readonly`; after execution (success or exception) it restores the
 * `DbParam` snapshot captured before entering the aspect.
 *
 * Use case: a single method temporarily needs to run on a specific data source (e.g. global config
 * scanning on a read-only replica) without polluting the routine routing of the entire service.
 *
 * For nested calls, the inner method only temporarily overrides the context; after returning it
 * restores the outer `DbParam` snapshot.
 *
 * @property value the data source key; an empty string means do not switch, only set the readonly flag.
 * @property readonly true means read-only; the aspect uses this to select a read-only replica.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class DsChange(val value: String = "", val readonly: Boolean = false)
