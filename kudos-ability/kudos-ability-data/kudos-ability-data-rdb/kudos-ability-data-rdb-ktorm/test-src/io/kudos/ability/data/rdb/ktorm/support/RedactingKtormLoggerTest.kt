package io.kudos.ability.data.rdb.ktorm.support

import org.ktorm.logging.Logger
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class RedactingKtormLoggerTest {

    @Test
    fun encryptedParametersAreRedactedButOrdinaryDiagnosticsRemainUseful() {
        val delegate = RecordingLogger()
        val logger = RedactingKtormLogger(delegate)

        logger.debug("Parameters: [alice(varchar), TOPSECRET(encrypted_varchar), 1(int)]")

        assertFalse(delegate.messages.single().contains("TOPSECRET"))
        assertTrue(delegate.messages.single().contains("alice(varchar)"))
        assertTrue(delegate.messages.single().contains("**redacted**(encrypted_varchar)"))
    }

    @Test
    fun traceIsDisabledBecauseDecryptedEntitiesMayContainSecrets() {
        val delegate = RecordingLogger()
        val logger = RedactingKtormLogger(delegate)

        assertFalse(logger.isTraceEnabled())
        logger.trace("UserAccount(authenticationKey=TOPSECRET)")
        assertTrue(delegate.messages.isEmpty())
    }

    private class RecordingLogger : Logger {
        val messages = mutableListOf<String>()
        override fun isTraceEnabled() = true
        override fun trace(msg: String, e: Throwable?) { messages += msg }
        override fun isDebugEnabled() = true
        override fun debug(msg: String, e: Throwable?) { messages += msg }
        override fun isInfoEnabled() = true
        override fun info(msg: String, e: Throwable?) { messages += msg }
        override fun isWarnEnabled() = true
        override fun warn(msg: String, e: Throwable?) { messages += msg }
        override fun isErrorEnabled() = true
        override fun error(msg: String, e: Throwable?) { messages += msg }
    }
}
