package io.kudos.context.support

/**
 * 常量类
 *
 * @author K
 * @since 1.0.0
 */
object Consts {

    /**
     * 缓存key默认分隔符
     */
    const val CACHE_KEY_DEFAULT_DELIMITER = "::"

    /**
     * 系统层默认编码。
     * 多租户 / 多微服务环境下，未显式配置时回落到这些 `default` 值。
     *
     * @author K
     * @since 1.0.0
     */
    object Sys {
        /** 默认门户编码 */
        const val DEFAULT_PORTAL_CODE = "default"

        /** 默认子系统编码 */
        const val DEFAULT_SUBSYSTEM_CODE = "default"

        /** 默认微服务编码 */
        const val DEFAULT_MICRO_SERVICE_CODE = "default"
    }

    /**
     * 跨进程透传的内部请求头 key。
     * 命名前缀统一加 `_`，与业务方的自定义 header 区分；网关或 Feign 拦截器需放行这些 key。
     *
     * @author K
     * @since 1.0.0
     */
    object RequestHeader {
        /** 标记当前请求来自 Feign 内部互调，避免重复鉴权 / 限流 */
        const val FEIGN_REQUEST: String = "_feign_request"
        /** 标记当前请求来自通知类（如回调、异步推送） */
        const val NOTIFY_REQUEST: String = "_notify_request"
        /** 子系统编码透传 */
        const val SUB_SYS_CODE: String = "_sub_sys_code"
        /** 租户 ID 透传 */
        const val TENANT_ID: String = "_tenant_id"
        /** 链路追踪 ID（UUID 串） */
        const val TRACE_KEY: String = "_UUID"
        /** 当前请求的 locale 透传 */
        const val LOCAL: String = "_LOCAL"
        /** 强制指定的数据源 ID，用于多数据源场景下手动路由 */
        const val DATASOURCE_ID: String = "_DATA_SOURCE_ID"
    }


}