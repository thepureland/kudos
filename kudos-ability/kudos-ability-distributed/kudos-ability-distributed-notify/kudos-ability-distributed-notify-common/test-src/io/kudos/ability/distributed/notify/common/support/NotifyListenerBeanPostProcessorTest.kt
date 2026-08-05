package io.kudos.ability.distributed.notify.common.support

import io.kudos.ability.distributed.notify.common.api.INotifyListener
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Unit tests for [NotifyListenerBeanPostProcessor].
 *
 * Covers:
 *  - [INotifyListener] beans are registered into [NotifyListenerItem] under the processor's namespace
 *  - the default-namespace constructor registers under [NotifyListenerItem.DEFAULT_NAMESPACE]
 *  - non-listener beans pass through unchanged and are never registered
 *  - the original bean instance is always returned (no proxying / replacement)
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class NotifyListenerBeanPostProcessorTest {

    private class TestListener(private val type: String) : INotifyListener {
        override fun notifyType(): String = type
        override fun notifyProcess(notifyMessageVo: NotifyMessageVo<out Serializable>) {}
    }

    @Test
    fun postProcess_registersListenerUnderGivenNamespace() {
        val processor = NotifyListenerBeanPostProcessor("bpp-ns-a")
        val listener = TestListener("notify.bpp.namespaced")

        val returned = processor.postProcessAfterInitialization(listener, "myListener")

        assertSame(listener, returned)
        assertSame(listener, NotifyListenerItem.get("bpp-ns-a", "notify.bpp.namespaced"))
        // Must not leak into other namespaces.
        assertNull(NotifyListenerItem.get("bpp-ns-other", "notify.bpp.namespaced"))
    }

    @Test
    fun postProcess_defaultConstructorRegistersUnderDefaultNamespace() {
        val processor = NotifyListenerBeanPostProcessor()
        val listener = TestListener("notify.bpp.default")

        processor.postProcessAfterInitialization(listener, "defaultListener")

        assertSame(listener, NotifyListenerItem.get(NotifyListenerItem.DEFAULT_NAMESPACE, "notify.bpp.default"))
    }

    @Test
    fun postProcess_nonListenerBeanPassesThroughWithoutRegistration() {
        val processor = NotifyListenerBeanPostProcessor("bpp-ns-plain")
        val plainBean = "just a string bean"

        val returned = processor.postProcessAfterInitialization(plainBean, "plainBean")

        assertSame(plainBean, returned)
        assertNull(NotifyListenerItem.get("bpp-ns-plain", "plainBean"))
    }

}
