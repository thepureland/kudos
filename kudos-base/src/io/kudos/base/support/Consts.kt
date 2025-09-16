package io.kudos.base.support


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

    object Suppress { // org.jetbrains.kotlin.diagnostics.Errors

        const val UNCHECKED_CAST = "UNCHECKED_CAST"

        const val UNUSED_PARAMETER = "UNUSED_PARAMETER"

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