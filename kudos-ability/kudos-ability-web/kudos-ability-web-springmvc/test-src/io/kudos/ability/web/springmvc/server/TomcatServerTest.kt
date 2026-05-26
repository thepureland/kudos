package io.kudos.ability.web.springmvc.server

import io.kudos.test.common.init.EnableKudosTest
import org.springframework.boot.test.context.SpringBootTest

/**
 * Test cases for the Tomcat container.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["kudos.ability.web.springmvc.server=TOMCAT"]
)
class TomcatServerTest : BaseWebServerTest()