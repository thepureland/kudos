package io.kudos.ability.web.ktor.init


/**
 * Ktor配置属性
 *
 * @author K
 * @since 1.0.0
 */
data class KtorProperties(
    val engine: Engine = Engine(),
    val plugins: Plugins = Plugins()
) {
    data class Engine(
        var name: String = "test",
        var port: Int = 0
    )

    data class Plugins(
        var contentNegotiation: Plugin = Plugin(),
        var statusPages: Plugin = Plugin(),
        var webSocket: Plugin = Plugin()
    ) {
        data class Plugin(
            var enabled: Boolean = true
        )
    }
}