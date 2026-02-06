package io.kudos.test.common.init

import io.kudos.context.kit.SpringKit
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener

/**
 * 在测试准备阶段将 SpringKit 的 applicationContext 同步为当前测试的 ApplicationContext。
 *
 * 根因：SpringKit 使用静态 applicationContext，仅在上下文**创建**时由
 * [io.kudos.context.spring.SpringContextInitializer] 设置；当 Spring 测试**复用**已缓存的上下文时，
 * 不会再次执行 ApplicationContextInitializer，导致 SpringKit 仍指向其它上下文，
 * 从而出现 NoSuchBeanDefinitionException。
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
