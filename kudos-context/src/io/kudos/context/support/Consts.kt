package io.kudos.context.support

/**
 * Constants.
 *
 * @author K
 * @since 1.0.0
 */
object Consts {

    /**
     * Default delimiter used in cache keys.
     */
    const val CACHE_KEY_DEFAULT_DELIMITER = "::"

    /**
     * System-level default codes.
     * In multi-tenant / multi-microservice environments, falls back to these `default` values when
     * not explicitly configured.
     *
     * @author K
     * @since 1.0.0
     */
    object Sys {
        /** Default portal code. */
        const val DEFAULT_PORTAL_CODE = "default"

        /** Default subsystem code. */
        const val DEFAULT_SUBSYSTEM_CODE = "default"

        /** Default microservice code. */
        const val DEFAULT_MICRO_SERVICE_CODE = "default"
    }

    /**
     * Internal request header keys passed across processes.
     * The naming uniformly prefixes `_` to distinguish them from business-defined headers; gateways
     * and Feign interceptors must allow these keys through.
     *
     * @author K
     * @since 1.0.0
     */
    object RequestHeader {
        /** Marks the current request as an internal Feign call, to avoid duplicate auth / rate limiting. */
        const val FEIGN_REQUEST: String = "_feign_request"
        /** Marks the current request as a notification (e.g. callback, async push). */
        const val NOTIFY_REQUEST: String = "_notify_request"
        /** Subsystem code passthrough. */
        const val SUB_SYS_CODE: String = "_sub_sys_code"
        /** Tenant ID passthrough. */
        const val TENANT_ID: String = "_tenant_id"
        /** Trace ID (UUID string). */
        const val TRACE_KEY: String = "_UUID"
        /** Locale of the current request passthrough. */
        const val LOCAL: String = "_LOCAL"
        /** Explicitly specified data source ID, for manual routing in multi-data-source scenarios. */
        const val DATASOURCE_ID: String = "_DATA_SOURCE_ID"
    }


}