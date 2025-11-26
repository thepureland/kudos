package io.kudos.ability.web.springmvc.server

import io.kudos.test.common.init.EnableKudosTest
import org.junit.jupiter.api.Disabled
import org.springframework.boot.test.context.SpringBootTest


/**
 * undertow容器测试用例
 *
 * @author K
 * @since 1.0.0
 */
@Disabled("springboot4不再支持undertow")
@EnableKudosTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["kudos.ability.web.springmvc.server=UNDERTOW"]
)
class UndertowServerTest : BaseWebServerTest()