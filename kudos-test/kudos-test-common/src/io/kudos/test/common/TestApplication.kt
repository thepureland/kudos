package io.kudos.test.common

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

/**
 * 单元测试的spring启动类
 *
 * @author K
 * @since 1.0.0
 */
@SpringBootApplication
@ComponentScan("io.kudos")
open class TestApplication