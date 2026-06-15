package io.kudos.context.lock

import io.kudos.base.enums.ienums.IErrorCodeEnum
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Supplier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * test for the Java-interop compatibility bridges (DefaultImpls) of the lock interfaces
 *
 * The Kotlin compiler emits XxxKt$DefaultImpls static bridges so that pre-existing Java callers and
 * older Kotlin binaries can still dispatch the interface default methods statically. These bridges
 * are part of the public binary contract of the module but are bypassed by normal Kotlin call sites,
 * so they are exercised here reflectively, exactly the way a legacy binary would.
 *
 * Coverage:
 * - ILeaseLockProvider$DefaultImpls.lockExecute (Supplier and Runnable bridges)
 * - ILockProvider$DefaultImpls.order bridge
 * - ILockProvider.Companion initialization
 *
 * @author K
 * @since 1.0.0
 */
internal class LockProviderJavaInteropTest {

    private class CountingLockProvider : ILockProvider<ReentrantLock> {
        val unlocked = mutableListOf<String>()
        override fun tryLock(lockKey: String, sec: Int) = true
        override fun unLock(key: String) {
            unlocked += key
        }

        override fun lock(key: String): ReentrantLock? = null
        override fun unLock(lock: Lock, key: String) {}
    }

    @Test
    fun leaseLockDefaultImplsBridgesDispatchToTheSameLogic() {
        val provider = CountingLockProvider()
        val defaultImpls = Class.forName("io.kudos.context.lock.ILeaseLockProvider\$DefaultImpls")

        val supplierBridge = defaultImpls.getMethod(
            "lockExecute",
            ILeaseLockProvider::class.java, String::class.java, Supplier::class.java,
            Int::class.javaPrimitiveType, IErrorCodeEnum::class.java
        )
        val supplierResult = supplierBridge.invoke(null, provider, "ik1", Supplier { "bridged" }, 10, null)
        assertEquals("bridged", supplierResult)
        assertEquals(listOf("ik1"), provider.unlocked, "the bridge must release the lock like the default method")

        val runnableBridge = defaultImpls.getMethod(
            "lockExecute",
            ILeaseLockProvider::class.java, String::class.java, Runnable::class.java,
            Int::class.javaPrimitiveType, IErrorCodeEnum::class.java
        )
        var ran = false
        runnableBridge.invoke(null, provider, "ik2", Runnable { ran = true }, 10, null)
        assertTrue(ran)
        assertEquals(listOf("ik1", "ik2"), provider.unlocked)
    }

    @Test
    fun lockProviderDefaultImplsLockExecuteBridgesDispatchToTheSameLogic() {
        // The sub-interface re-declares the inherited default bridges with ILockProvider receivers
        val provider = CountingLockProvider()
        val defaultImpls = Class.forName("io.kudos.context.lock.ILockProvider\$DefaultImpls")

        val supplierBridge = defaultImpls.getMethod(
            "lockExecute",
            ILockProvider::class.java, String::class.java, Supplier::class.java,
            Int::class.javaPrimitiveType, IErrorCodeEnum::class.java
        )
        assertEquals("bridged2", supplierBridge.invoke(null, provider, "pk1", Supplier { "bridged2" }, 10, null))
        assertEquals(listOf("pk1"), provider.unlocked)

        val runnableBridge = defaultImpls.getMethod(
            "lockExecute",
            ILockProvider::class.java, String::class.java, Runnable::class.java,
            Int::class.javaPrimitiveType, IErrorCodeEnum::class.java
        )
        var ran = false
        runnableBridge.invoke(null, provider, "pk2", Runnable { ran = true }, 10, null)
        assertTrue(ran)
        assertEquals(listOf("pk1", "pk2"), provider.unlocked)
    }

    @Test
    fun lockProviderDefaultImplsOrderBridgeReturnsDefault() {
        val defaultImpls = Class.forName("io.kudos.context.lock.ILockProvider\$DefaultImpls")
        val orderBridge = defaultImpls.getMethod("order", ILockProvider::class.java)
        assertEquals(99, orderBridge.invoke(null, CountingLockProvider()))
    }

    @Test
    fun companionObjectIsInitializable() {
        assertNotNull(ILockProvider.Companion)
        assertEquals("failedLockProvider", ILockProvider.BEAN_NAME)
    }
}
