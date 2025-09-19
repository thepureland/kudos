package io.kudos.ability.distributed.stream.common.model.vo

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import org.springframework.messaging.MessageHeaders

/**
 * 自定义的streamHeader
 */
class StreamHeader {
    var destination: String? = null //消息主題
    var tenantId: String? = null
    var dataSourceId: Int? = null
    var userId: String? = null
    var username: String? = null
    private var _datasourceTenantId: String? = null

    fun get_datasourceTenantId(): String? {
        return _datasourceTenantId
    }

    fun set_datasourceTenantId(_datasourceTenantId: String?) {
        this._datasourceTenantId = _datasourceTenantId
    }

    companion object {
        const val TOPIC_KEY: String = "destination"
        const val TENANT_ID_KEY: String = "tenantId"
        const val DATA_SOURCE_ID_KEY: String = "dataSourceId"
        const val USER_ID_KEY: String = "userId"
        const val USERNAME_KEY: String = "username"
        const val DATASOURCE_TENANT_ID: String = "_datasourceTenantId"
        const val SCST_BIND_NAME: String = "scst_produce_bind_name_"

        fun initHeader(destination: String?): StreamHeader {
            val context = KudosContextHolder.get()
            val header = StreamHeader()
            header.destination = destination
//            header.username = context.user //TODO
            header.userId = context.user?.id
            header.tenantId = context.tenantId
            header.set_datasourceTenantId(context._datasourceTenantId)
            return header
        }

        fun toContextParam(headers: MessageHeaders): KudosContext {
            return KudosContext().apply {
//                user //TODO
                tenantId = headers.get(USER_ID_KEY) as String?
                dataSourceId = headers.get(DATA_SOURCE_ID_KEY) as String?
//                USERNAME_KEY = headers.get(USERNAME_KEY) as String?
                _datasourceTenantId = headers.get(DATASOURCE_TENANT_ID) as String?
            }
        }
    }
}
