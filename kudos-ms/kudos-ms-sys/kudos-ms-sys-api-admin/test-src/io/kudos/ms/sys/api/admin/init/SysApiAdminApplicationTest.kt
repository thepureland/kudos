package io.kudos.ms.sys.api.admin.init

import io.kudos.test.container.containers.H2TestContainer
import io.kudos.test.container.containers.RedisTestContainer
import org.springframework.boot.SpringApplication

fun main(args : Array<String>) {

    // 构造中间件环境
    H2TestContainer.startIfNeeded(null)
    RedisTestContainer.startIfNeeded(null)

    // 运行主应用
    SpringApplication.run(SysApiAdminApplication::class.java, *args)
}