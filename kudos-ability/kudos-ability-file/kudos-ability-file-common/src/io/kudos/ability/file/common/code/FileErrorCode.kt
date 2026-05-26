package io.kudos.ability.file.common.code

import io.kudos.base.enums.ienums.IErrorCodeEnum
import java.text.MessageFormat

/**
 * File error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class FileErrorCode(
    override val code: String,
    override val defaultDisplayText: String
) : IErrorCodeEnum {

    FILE_INVALID_ACCESS_KEY("FS00000000", "Invalid authentication information"),
    FILE_ACCESS_ERROR("FS00000001", "File access error"),
    FILE_ACCESS_DENY("FS00000002", "File access denied"),
    FILE_NO_EXISTS("FS00000003", "File does not exist"),
    FILE_UPLOAD_FAIL("FS00000004", "File upload failed"),
    FILE_DELETE_FAIL("FS00000005", "File deletion failed");

    fun getMessage(vararg params: Any): String {
        return MessageFormat.format(this.displayText, *params)
    }

    override val i18nKeyPrefix: String
        get() = ""
}
