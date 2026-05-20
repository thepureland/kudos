package io.kudos.ms.sys.core.platform.service.impl

import io.kudos.base.logger.ILog
import io.kudos.base.model.contract.entity.IIdEntity

/**
 * 从 update 入参中抽取主键。约 13 个 service 里的 `requireXxxId(any)` 私有 helper 完全一致——只是
 * `entityLabel` 不同——抽出统一收口；要求入参实现 [IIdEntity] 且 `id` 是 `String`。
 *
 * @param any update 入参（通常是 service 的 VO / form 对象）
 * @param entityLabel 实体的中文显示名，用于失败时的 error 消息（如 "租户"、"域名"）
 * @return 主键
 * @throws IllegalStateException 入参未实现 [IIdEntity] 或 id 不是 String
 */
internal fun requireStringId(any: Any, entityLabel: String): String =
    (any as? IIdEntity<*>)?.id as? String
        ?: error("更新${entityLabel}时不支持的入参类型: ${any::class.qualifiedName}")

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

internal fun completeCrudInsert(
    log: ILog,
    successMessage: String,
    onSuccess: () -> Unit = {},
) {
    log.debug(successMessage)
    onSuccess()
}
