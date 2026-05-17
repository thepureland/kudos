package io.kudos.ms.sys.core.param.event

/**
 * 参数（`sys_param`）领域事件。
 *
 * 参数缓存以 `(atomicServiceCode, paramName)` 为复合 key，所以删除事件需要携带该维度信息——
 * DB 删除后这两个字段无法回查。
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

/** 删除事件携带 (atomicServiceCode, paramName) 维度以支持缓存按复合 key 失效。 */
data class SysParamDeleted(
    override val id: String,
    val atomicServiceCode: String,
    val paramName: String,
) : SysParamEvent

/** 批量删除事件：携带每条记录的维度对，顺序与 ids 不必对齐。 */
data class SysParamBatchDeleted(
    val ids: Collection<String>,
    val moduleAndNames: List<Pair<String, String>>,
) : SysParamEvent {
    override val id: String get() = ids.first()
}
