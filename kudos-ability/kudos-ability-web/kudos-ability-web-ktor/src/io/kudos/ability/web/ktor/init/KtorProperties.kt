package io.kudos.ability.web.ktor.init


/**
 * Ktor 配置属性，对应 `kudos.ability.web.ktor.*`。
 *
 * @property engine 引擎选择 + 端口
 * @property plugins 各 ktor 插件的启用开关
 * @author K
 * @since 1.0.0
 */
data class KtorProperties(
    val engine: Engine = Engine(),
    val plugins: Plugins = Plugins()
) {
    /**
     * 引擎配置。
     *
     * @property name 引擎名（大小写不敏感）：`cio` / `netty` / `jetty` / `tomcat` / `test`。
     *   `test` 表示不启动真实引擎，由测试用例自行装配 [io.ktor.server.testing.testApplication]。
     * @property port 监听端口；0 表示随机端口（注意：随机端口主要供测试使用，生产请指定）。
     */
    data class Engine(
        var name: String = "test",
        var port: Int = 0
    )

    /**
     * 插件开关合集。每个插件的 [Plugin.enabled] 默认 true；按需关闭以减少冷启动开销。
     */
    data class Plugins(
        /** Ktor [io.ktor.server.plugins.contentnegotiation.ContentNegotiation] —— JSON 序列化。 */
        var contentNegotiation: Plugin = Plugin(),
        /** Ktor [io.ktor.server.plugins.statuspages.StatusPages] —— 状态码 / 异常统一响应。 */
        var statusPages: Plugin = Plugin(),
        /** Ktor [io.ktor.server.websocket.WebSockets] —— WebSocket 支持。 */
        var webSocket: Plugin = Plugin()
    ) {
        /** 单个插件开关。`enabled = false` 时本模块不会 `install` 该插件。 */
        data class Plugin(
            var enabled: Boolean = true
        )
    }
}