package io.kudos.ability.data.rdb.jdbc.context

import java.io.Serializable

/**
 * "Session parameters" for database routing; each thread holds one, managed by [DbContext].
 *
 * The routing advices (`DsChangeAspect` / `TenantDsChangeAspect` / `DynamicDataSourceInterceptor`)
 * use this object's fields to decide which data source the current method call
 * routes to, whether it is read-only, and whether to log. `Serializable` is kept
 * as a reservation for cross-process delivery (e.g. serialization to MQ messages
 * or caches); the main path does not rely on it today.
 *
 * A data class: `copy()` produces the snapshot the aspects capture on entry and restore on exit.
 *
 * @property forcedDs force-specified data source key. When non-null, advices skip normal routing and switch directly to this key.
 * @property enableLog whether to log routing decisions (INFO level). Normally off on debug paths; turn on manually when investigating weird routing.
 * @property readonly whether to use a read-only replica. Useful on certain paths (cache warmup, report queries) to relieve master-DB load.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class DbParam(
    var forcedDs: String? = null,
    var enableLog: Boolean = false,
    var readonly: Boolean = false,
) : Serializable {

    companion object {
        /** [Serializable] compatibility field, avoiding deserialization failures across JDKs. */
        private const val serialVersionUID = -3788770245369263297L
    }
}
