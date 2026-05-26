package io.kudos.ability.web.springmvc.server

import io.kudos.test.common.init.EnableKudosTest
import org.junit.jupiter.api.Disabled
import org.springframework.boot.test.context.SpringBootTest


/**
 * Test cases for the Undertow container.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Disabled("Spring Boot 4 no longer supports Undertow")
@EnableKudosTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["kudos.ability.web.springmvc.server=UNDERTOW"]
)
class UndertowServerTest : BaseWebServerTest()