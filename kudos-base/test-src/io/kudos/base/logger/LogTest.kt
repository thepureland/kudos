package io.kudos.base.logger

import kotlin.test.Test

/**
 * 日志单元测试
 *
 * @author K
 * @since 1.0.0
 */
class LogTest {

    private val log = LogFactory.getLog(this)

    @Test
    fun log() {
        log.debug("test")
    }

}