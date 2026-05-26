package io.kudos.ability.web.ktor.init


/**
 * Ktor configuration properties, corresponding to `kudos.ability.web.ktor.*`.
 *
 * @property engine engine selection + port
 * @property plugins enable switches for each ktor plugin
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class KtorProperties(
    val engine: Engine = Engine(),
    val plugins: Plugins = Plugins()
) {
    /**
     * Engine configuration.
     *
     * @property name engine name (case-insensitive): `cio` / `netty` / `jetty` / `tomcat` / `test`.
     *   `test` means no real engine is started; tests wire up [io.ktor.server.testing.testApplication] themselves.
     * @property port listening port; 0 means a random port (note: random ports are mainly for testing, specify one in production).
     */
    data class Engine(
        var name: String = "test",
        var port: Int = 0
    )

    /**
     * Collection of plugin switches. Each plugin's [Plugin.enabled] defaults to true;
     * disable as needed to reduce cold-start overhead.
     */
    data class Plugins(
        /** Ktor [io.ktor.server.plugins.contentnegotiation.ContentNegotiation] — JSON serialization. */
        var contentNegotiation: Plugin = Plugin(),
        /** Ktor [io.ktor.server.plugins.statuspages.StatusPages] — unified status code / exception responses. */
        var statusPages: Plugin = Plugin(),
        /** Ktor [io.ktor.server.websocket.WebSockets] — WebSocket support. */
        var webSocket: Plugin = Plugin()
    ) {
        /** Switch for an individual plugin. When `enabled = false` this module will not `install` the plugin. */
        data class Plugin(
            var enabled: Boolean = true
        )
    }
}