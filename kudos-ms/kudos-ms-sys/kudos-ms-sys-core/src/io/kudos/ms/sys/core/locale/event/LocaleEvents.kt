package io.kudos.ms.sys.core.locale.event

/**
 * Domain events for the language/locale dictionary (`sys_locale`).
 *
 * @author K
 * @since 1.0.0
 */
sealed interface SysLocaleEvent {
    val id: String
}

data class SysLocaleInserted(override val id: String) : SysLocaleEvent
data class SysLocaleUpdated(override val id: String) : SysLocaleEvent

/** Delete event carries the code so the cache can be invalidated by code key. */
data class SysLocaleDeleted(
    override val id: String,
    val code: String,
) : SysLocaleEvent

/** Batch delete: carries the set of codes for the deleted records. */
data class SysLocaleBatchDeleted(
    val ids: Collection<String>,
    val codes: Set<String>,
) : SysLocaleEvent {
    override val id: String get() = ids.first()
}
