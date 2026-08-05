package io.kudos.context.init

import io.kudos.contexttest.fixture.CountingNameInitializer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for IComponentInitializer default methods
 *
 * Coverage: the default beforeInit / afterInit logging implementations execute without error
 * and use getComponentName() (verified by counting invocations).
 *
 * Note: the counting implementation lives in io.kudos.contexttest.fixture — an anonymous or nested
 * implementation inside io.kudos.context.* would be picked up by ComponentInitializerSelector's
 * classpath scan and break @EnableKudosTest contexts in this JVM (anonymous classes have no
 * qualifiedName and would be imported as "Unknown").
 *
 * @author K
 * @since 1.0.0
 */
internal class IComponentInitializerTest {

    @Test
    fun defaultLifecycleMethodsRunAndQueryComponentName() {
        val initializer = CountingNameInitializer()
        initializer.beforeInit()
        initializer.afterInit()
        assertEquals(2, initializer.nameQueries, "both default lifecycle logs should include the component name")
    }

    @Test
    fun javaInteropDefaultImplsBridgesDispatchTheSameLogic() {
        // The compiler emits IComponentInitializer$DefaultImpls static bridges for Java callers /
        // older binaries; exercise them the way such a caller would.
        val initializer = CountingNameInitializer()
        val defaultImpls = Class.forName("io.kudos.context.init.IComponentInitializer\$DefaultImpls")
        defaultImpls.getMethod("beforeInit", IComponentInitializer::class.java).invoke(null, initializer)
        defaultImpls.getMethod("afterInit", IComponentInitializer::class.java).invoke(null, initializer)
        assertEquals(2, initializer.nameQueries)
    }
}
