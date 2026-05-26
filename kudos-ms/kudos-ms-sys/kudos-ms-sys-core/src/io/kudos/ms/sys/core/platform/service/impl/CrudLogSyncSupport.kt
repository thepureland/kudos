package io.kudos.ms.sys.core.platform.service.impl

import io.kudos.base.logger.ILog
import io.kudos.base.model.contract.entity.IIdEntity

/**
 * Extract the primary key from an update parameter. About 13 services have an identical
 * private `requireXxxId(any)` helper, differing only by `entityLabel`; this is the unified entry point.
 * The argument must implement [IIdEntity] with `id` of type `String`.
 *
 * @param any update parameter (typically the service's VO / form object)
 * @param entityLabel human-readable entity label, used in error messages (e.g. "tenant", "domain")
 * @return the primary key
 * @throws IllegalStateException when the argument does not implement [IIdEntity] or id is not a String
 */
internal fun requireStringId(any: Any, entityLabel: String): String =
    (any as? IIdEntity<*>)?.id as? String
        ?: error("Unsupported argument type when updating ${entityLabel}: ${any::class.qualifiedName}")

/**
 * Finalize an update / delete operation: log according to [success], then invoke [onSuccess]
 * on the success path (typical use: publish events, write to a ThreadLocal cache, and similar side effects).
 *
 * Extracted because update / delete methods across ms-sys-core services share the
 * "if (success) { log.debug + publish event } else { log.error }" template; duplicating it inline
 * is ugly and prone to dropping a log level.
 *
 * inline avoids allocating an extra [Function0] object at the call site (micro-optimization for hot paths).
 *
 * @param success success flag returned by the DAO layer
 * @param log the caller service's own logger (preserves the caller's category name)
 * @param successMessage debug message on success
 * @param failureMessage error message on failure
 * @param onSuccess side effect triggered only when [success] is true (e.g. publish event, update cache)
 * @return original [success] value, allowing chained `return completeCrudUpdate(...)`
 * @author K
 * @since 1.0.0
 */
internal inline fun completeCrudUpdate(
    success: Boolean,
    log: ILog,
    successMessage: String,
    failureMessage: String,
    onSuccess: () -> Unit = {},
): Boolean {
    if (success) {
        log.debug(successMessage)
        onSuccess()
    } else {
        log.error(failureMessage)
    }
    return success
}

/**
 * Finalize an insert operation: log debug message and invoke [onSuccess] (typically publishing an "inserted" event).
 *
 * There is no "failure branch" — insert failures are raised as exceptions by the upstream BaseCrudService,
 * so this method is never reached on failure.
 *
 * @param log the caller service's own logger
 * @param successMessage debug message on success
 * @param onSuccess side effect callback
 * @author K
 * @since 1.0.0
 */
internal fun completeCrudInsert(
    log: ILog,
    successMessage: String,
    onSuccess: () -> Unit = {},
) {
    log.debug(successMessage)
    onSuccess()
}
