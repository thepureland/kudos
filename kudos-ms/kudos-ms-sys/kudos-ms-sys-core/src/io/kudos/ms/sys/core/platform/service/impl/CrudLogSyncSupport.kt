package io.kudos.ms.sys.core.platform.service.impl
import io.kudos.base.logger.ILog

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
