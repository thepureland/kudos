package io.kudos.ms.sys.core.i18n.event

/**
 * Domain events for i18n entries (`sys_i18n`).
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysI18nEvent {
    val id: String
}

data class SysI18nInserted(override val id: String) : SysI18nEvent
data class SysI18nUpdated(override val id: String) : SysI18nEvent
data class SysI18nDeleted(override val id: String) : SysI18nEvent
data class SysI18nBatchDeleted(val ids: Collection<String>) : SysI18nEvent {
    override val id: String get() = ids.first()
}
