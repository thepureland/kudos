package io.kudos.ms.sys.core.platform.service.impl

import io.kudos.base.logger.ILog

/**
 * 收尾 update / delete 类操作：按 [success] 分两路打日志，成功路径再触发 [onSuccess]
 *（典型用途：发事件、写 ThreadLocal cache 之类的副作用）。
 *
 * 抽出来是因为 ms-sys-core 各 Service 的 update / delete 普遍是
 * "if (success) { log.debug + 发事件 } else { log.error }" 模板——
 * 行内重复一遍既丑也容易漏 log 等级。
 *
 * inline 是为避免 lambda 在调用点产生额外 [Function0] 对象（高频调用路径上的微优化）。
 *
 * @param success DAO 层返回的成功标志
 * @param log 调用方 service 自己的 logger（沿用调用方分类名）
 * @param successMessage 成功时的 debug 消息
 * @param failureMessage 失败时的 error 消息
 * @param onSuccess 仅在 [success]=true 时触发的副作用（如发 event、更新缓存）
 * @return [success] 原值，便于链式 `return completeCrudUpdate(...)`
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
 * 收尾 insert 操作：直接打 debug 日志 + 触发 [onSuccess]（通常是发"已新建"事件）。
 *
 * 没有"失败分支"——insert 失败由上游 BaseCrudService 抛异常处理，不会走到这里。
 *
 * @param log 调用方 service 自己的 logger
 * @param successMessage 成功时的 debug 消息
 * @param onSuccess 副作用回调
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
