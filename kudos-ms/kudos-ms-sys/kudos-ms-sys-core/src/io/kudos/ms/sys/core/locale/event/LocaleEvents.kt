package io.kudos.ms.sys.core.locale.event

/**
 * 语言/区域字典（`sys_locale`）领域事件
 *
 * @author K
 * @since 1.0.0
 */
sealed interface SysLocaleEvent {
    val id: String
}

data class SysLocaleInserted(override val id: String) : SysLocaleEvent
data class SysLocaleUpdated(override val id: String) : SysLocaleEvent

/** 删除事件携带 code，便于按 code key 失效缓存。 */
data class SysLocaleDeleted(
    override val id: String,
    val code: String,
) : SysLocaleEvent

/** 批量删除：携带每条记录的 code 集合。 */
data class SysLocaleBatchDeleted(
    val ids: Collection<String>,
    val codes: Set<String>,
) : SysLocaleEvent {
    override val id: String get() = ids.first()
}
