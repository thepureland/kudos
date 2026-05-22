package io.kudos.ability.distributed.config.nacos.listener

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * [AbstractConfigChangeListener] hook 顺序与异常回调测试。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class AbstractConfigChangeListenerTest {

    @Test
    fun receiveConfigInfo_invokesHooksAroundBusinessHandler() {
        val calls = mutableListOf<String>()
        val listener = object : AbstractConfigChangeListener() {
            override fun beforeConfigChanged(configInfo: String?) {
                calls += "before:$configInfo"
            }

            override fun onConfigChanged(configInfo: String?) {
                calls += "change:$configInfo"
            }

            override fun afterConfigChanged(configInfo: String?, cause: Throwable?) {
                calls += "after:$configInfo:${cause?.javaClass?.simpleName}"
            }
        }

        listener.receiveConfigInfo("v1")

        assertEquals(listOf("before:v1", "change:v1", "after:v1:null"), calls)
    }

    @Test
    fun receiveConfigInfo_invokesAfterHookWhenBusinessHandlerFails() {
        val calls = mutableListOf<String>()
        val listener = object : AbstractConfigChangeListener() {
            override fun beforeConfigChanged(configInfo: String?) {
                calls += "before"
            }

            override fun onConfigChanged(configInfo: String?) {
                throw IllegalStateException("boom")
            }

            override fun afterConfigChanged(configInfo: String?, cause: Throwable?) {
                calls += "after:${cause?.javaClass?.simpleName}"
            }
        }

        assertFailsWith<IllegalStateException> {
            listener.receiveConfigInfo("v2")
        }

        assertEquals(listOf("before", "after:IllegalStateException"), calls)
    }
}
