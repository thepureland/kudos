package io.kudos.ability.comm.websocket.spring.server

import io.kudos.ability.comm.websocket.spring.init.SpringWebSocketAutoConfiguration
import io.kudos.test.common.init.EnableKudosTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `kudos.ability.comm.websocket.spring.enabled=false` must actually switch the module off.
 *
 * Its own context, because the switch is a `@ConditionalOnProperty` evaluated once at startup — there
 * is no way to observe it from the main end-to-end suite, and a condition that silently never matches
 * (or never *stops* matching) is exactly the kind of defect no unit test reaches.
 *
 * The endpoint beans are still imported here on purpose: the assertion is that they are **not mounted**,
 * which is only meaningful if they exist and something chose not to register them.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["kudos.ability.comm.websocket.spring.enabled=false"],
)
@Import(WebSocketTestEndpoints::class)
internal class SpringWebSocketDisabledTest {

    @org.springframework.boot.test.web.server.LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun whenDisabled_theAutoConfigurationIsNotLoaded() {
        assertEquals(
            0,
            applicationContext.getBeanNamesForType(SpringWebSocketAutoConfiguration::class.java).size,
            "The property is what decides whether this module contributes anything at all",
        )
    }

    @Test
    fun whenDisabled_noEndpointAnswersAHandshake() {
        val failure = CollectingClient.connectExpectingFailure(
            port, "${WebSocketTestEndpoints.ECHO_PATH}?token=alice",
        )

        assertTrue(failure.messageChain().contains("404"),
            "An endpoint that was never registered must 404, not upgrade. Got: ${failure.messageChain()}")
    }
}
