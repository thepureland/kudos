package io.kudos.context.support

/**
 * 常量類
 *
 * @author K
 * @since 1.0.0
 */
object Consts {

    /**
     * 缓存key默认分隔符
     */
    const val CACHE_KEY_DEFAULT_DELIMITER = "::"

    object Sys {
        /** 默认门户编码 */
        const val DEFAULT_PORTAL_CODE = "default"

        /** 默认子系统编码 */
        const val DEFAULT_SUBSYSTEM_CODE = "default"

        /** 默认微服务编码 */
        const val DEFAULT_MICRO_SERVICE_CODE = "default"
    }

    object RequestHeader {
        const val FEIGN_REQUEST: String = "_feign_request"
        const val NOTIFY_REQUEST: String = "_notify_request"
        const val SUB_SYS_CODE: String = "_sub_sys_code"
        const val TENANT_ID: String = "_tenant_id"
        const val TRACE_KEY: String = "_UUID"
        const val LOCAL: String = "_LOCAL"
        const val DATASOURCE_ID: String = "_DATA_SOURCE_ID"
    }


}