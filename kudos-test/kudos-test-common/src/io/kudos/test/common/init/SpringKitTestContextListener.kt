package io.kudos.test.common.init

import io.kudos.context.kit.SpringKit
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener

/**
 * During test preparation, syncs SpringKit's applicationContext with the current test's ApplicationContext.
 *
 * Root cause: SpringKit uses a static applicationContext that is set by
 * [io.kudos.context.spring.SpringContextInitializer] only when the context is **created**. When the Spring
 * test framework **reuses** a cached context, the ApplicationContextInitializer is not run again, leaving
 * SpringKit pointing at another context and causing NoSuchBeanDefinitionException.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
class SpringKitTestContextListener : TestExecutionListener {

    override fun prepareTestInstance(testContext: TestContext) {
        SpringKit.applicationContext = testContext.applicationContext
    }
}
