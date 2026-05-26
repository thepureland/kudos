package io.kudos.base.logger

import kotlin.test.Test

/**
 * Unit tests for logging.
 *
 * @author K
 * @since 1.0.0
 */
class LogTest {

    private val log = LogFactory.getLog(this::class)

    @Test
    fun log() {
        log.debug("test")
    }

}