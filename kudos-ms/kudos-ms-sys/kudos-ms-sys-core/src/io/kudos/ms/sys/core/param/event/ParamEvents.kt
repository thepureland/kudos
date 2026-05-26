package io.kudos.ms.sys.core.param.event

/**
 * Domain events for parameters (`sys_param`).
 *
 * The parameter cache is keyed by the composite `(atomicServiceCode, paramName)`, so delete events
 * must carry this dimension information — these two fields cannot be looked up after the DB row is deleted.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysParamEvent {
    val id: String
}

data class SysParamInserted(override val id: String) : SysParamEvent
data class SysParamUpdated(override val id: String) : SysParamEvent

/** Delete event carries the (atomicServiceCode, paramName) dimension to support cache invalidation by composite key. */
data class SysParamDeleted(
    override val id: String,
    val atomicServiceCode: String,
    val paramName: String,
) : SysParamEvent

/** Batch delete event: carries the dimension pair for each record; ordering need not align with ids. */
data class SysParamBatchDeleted(
    val ids: Collection<String>,
    val moduleAndNames: List<Pair<String, String>>,
) : SysParamEvent {
    override val id: String get() = ids.first()
}
