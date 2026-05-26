package io.kudos.ability.data.rdb.jdbc.context

import java.io.Serializable

/**
 * "Session parameters" for database routing; each thread holds one, managed by [DbContext].
 *
 * Aspects (`DsChangeAspect` / `TenantDsChangeAspect` / `DynamicDataSourceAspect`)
 * use this object's fields to decide which data source the current method call
 * routes to, whether it is read-only, and whether to log. `Serializable` is kept
 * as a reservation for cross-process delivery (e.g. serialization to MQ messages
 * or caches); the main path does not rely on it today.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class DbParam : Serializable {

    /** Force-specified data source key. When non-null, aspects skip normal routing and switch directly to this key. */
    var forcedDs: String? = null

    /** Whether to log routing decisions (INFO level). Normally off on debug paths; turn on manually when investigating weird routing. */
    var enableLog: Boolean = false

    /** Whether to use a read-only replica. Useful on certain paths (cache warmup, report queries) to relieve master-DB load. */
    var readonly: Boolean = false

    /** Creates a snapshot of the current routing parameters, used to restore the outer context after nested aspect calls. */
    fun copy(): DbParam {
        return DbParam().also {
            it.forcedDs = forcedDs
            it.enableLog = enableLog
            it.readonly = readonly
        }
    }

    companion object {
        /** [Serializable] compatibility field, avoiding deserialization failures across JDKs. */
        private const val serialVersionUID = -3788770245369263297L
    }
}
