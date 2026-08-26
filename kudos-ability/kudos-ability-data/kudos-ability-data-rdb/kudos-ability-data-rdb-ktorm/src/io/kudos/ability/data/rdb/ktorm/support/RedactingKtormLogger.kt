package io.kudos.ability.data.rdb.ktorm.support

import org.ktorm.logging.Logger

/** Prevents Ktorm diagnostics from exposing decrypted entities or encrypted-column plaintext inputs. */
class RedactingKtormLogger(
    private val delegate: Logger,
) : Logger by delegate {

    /** Entity TRACE output can contain decrypted credential fields, so it is intentionally disabled. */
    override fun isTraceEnabled(): Boolean = false

    override fun trace(msg: String, e: Throwable?) = Unit

    override fun debug(msg: String, e: Throwable?) {
        delegate.debug(redact(msg), e)
    }

    internal fun redact(message: String): String {
        if (!message.startsWith(PARAMETERS_PREFIX)) return message
        return ENCRYPTED_PARAMETER.replace(message) { "**redacted**(${EncryptedVarcharSqlType.ENCRYPTED_TYPE_NAME})" }
    }

    private companion object {
        const val PARAMETERS_PREFIX = "Parameters:"
        val ENCRYPTED_PARAMETER = Regex("[^,\\[\\]]*\\(${EncryptedVarcharSqlType.ENCRYPTED_TYPE_NAME}\\)")
    }
}
