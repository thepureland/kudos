package io.kudos.ability.web.springmvc.server

import io.kudos.test.common.init.EnableKudosTest
import org.springframework.boot.test.context.SpringBootTest

/**
 * jetty容器测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["kudos.ability.web.springmvc.server=JETTY"]
)
class JettyServerTest : BaseWebServerTest()