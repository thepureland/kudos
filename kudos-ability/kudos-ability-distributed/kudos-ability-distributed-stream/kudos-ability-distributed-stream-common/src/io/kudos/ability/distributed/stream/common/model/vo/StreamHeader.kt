package io.kudos.ability.distributed.stream.common.model.vo

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import org.springframework.messaging.MessageHeaders

/**
 * Custom header for Stream messages — carries key [KudosContext] fields
 * (tenantId / userId / dataSourceId / _datasourceTenantId) along with the message
 * so the consumer side can rebuild the caller's context.
 *
 * @author K
 * @since 1.0.0
 */
class StreamHeader {
    /** Message topic / binding destination. */
    var destination: String? = null
    var tenantId: String? = null
    var dataSourceId: Int? = null
    var userId: String? = null
    var username: String? = null

    /**
     * Data source tenant id (auxiliary routing field for kudos multi-tenant scenarios).
     *
     * **The leading underscore is a legacy wire-format convention** —
     * [KudosContext._datasourceTenantId] and the jdbc module `DsContextProcessor` both
     * use this name; [StreamProducerHelper.createMessage] extracts it via
     * `BeanKit.extract(header)` by property name into MessageHeaders, aligned with the
     * [DATASOURCE_TENANT_ID] constant below. Do not rename, otherwise this field will
     * be lost in cross-service messages.
     */
    @Suppress("PropertyName")
    var _datasourceTenantId: String? = null

    companion object {
        const val TOPIC_KEY: String = "destination"
        const val TENANT_ID_KEY: String = "tenantId"
        const val DATA_SOURCE_ID_KEY: String = "dataSourceId"
        const val USER_ID_KEY: String = "userId"
        const val USERNAME_KEY: String = "username"
        const val DATASOURCE_TENANT_ID: String = "_datasourceTenantId"
        const val SCST_BIND_NAME: String = "scst_produce_bind_name_"

        /** Initializes a [StreamHeader] from the current thread's [KudosContext]; called by the producer side before send. */
        fun initHeader(destination: String?): StreamHeader {
            val context = KudosContextHolder.get()
            return StreamHeader().apply {
                this.destination = destination
                this.userId = context.user?.id
                this.tenantId = context.tenantId
                this._datasourceTenantId = context._datasourceTenantId
            }
        }

        /**
         * Restores a [KudosContext] from [MessageHeaders] — used on the consumer side.
         *
         * Historical bugs (fixed): the old implementation
         * - `tenantId = headers.get(USER_ID_KEY)` mistakenly assigned the userId value to tenantId.
         * - `dataSourceId = headers.get(DATA_SOURCE_ID_KEY) as String?` had a type mismatch with
         *   StreamHeader.dataSourceId declared as `Int?` (it did not blow up only because
         *   KudosContext.dataSourceId is String?).
         * - The comment `USERNAME_KEY = headers.get(USERNAME_KEY)` was an invalid syntax placeholder.
         *
         * There are currently no call sites — this is a latent bug that would surface if used.
         * Fixed and retained for when the consumer side needs to restore the context.
         */
        fun toContextParam(headers: MessageHeaders): KudosContext = KudosContext().apply {
            tenantId = headers[TENANT_ID_KEY] as String?
            dataSourceId = headers[DATA_SOURCE_ID_KEY] as String?
            _datasourceTenantId = headers[DATASOURCE_TENANT_ID] as String?
        }
    }
}