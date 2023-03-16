package io.kudos.context.init

import io.kudos.base.logger.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.ComponentScan
import javax.annotation.PostConstruct


@ComponentScan(basePackages = ["io.kudos.context"])
@AutoConfigureOrder(-1000)
open class ContextAutoConfiguration {

    private val logger = LoggerFactory.getLogger(this)

    @PostConstruct
    open fun init() {
        logger.info("【kudos-context】初始化完成.")
    }

}