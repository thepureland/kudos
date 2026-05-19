package io.kudos.ability.distributed.stream.common.model.vo

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import org.springframework.messaging.MessageHeaders

/**
 * Stream 消息自定义 header——把 [KudosContext] 关键字段（tenantId / userId / dataSourceId
 * / _datasourceTenantId）随消息一起带到消费端，让消费端能重建调用方的上下文。
 *
 * @author K
 * @since 1.0.0
 */
class StreamHeader {
    /** 消息主题 / binding destination。 */
    var destination: String? = null
    var tenantId: String? = null
    var dataSourceId: Int? = null
    var userId: String? = null
    var username: String? = null

    /**
     * 数据源租户 id（kudos 多租户场景下的辅助路由字段）。
     *
     * **下划线前缀是历史 wire format 约定**——[KudosContext._datasourceTenantId] / jdbc 模块
     * `DsContextProcessor` 都用这个名字；[StreamProducerHelper.createMessage] 通过
     * `BeanKit.extract(header)` 按 property name 抽取到 MessageHeaders，与下方常量
     * [DATASOURCE_TENANT_ID] 对齐。不要重命名，否则跨服务消息丢失这个字段。
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

        /** 用当前线程 [KudosContext] 初始化 [StreamHeader]，由 producer 端发送前调用。 */
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
         * 从 [MessageHeaders] 还原 [KudosContext]——consumer 端用。
         *
         * 历史 bug（已修）：旧实现
         * - `tenantId = headers.get(USER_ID_KEY)` 错把 userId 值赋给 tenantId
         * - `dataSourceId = headers.get(DATA_SOURCE_ID_KEY) as String?` 类型与 StreamHeader.dataSourceId
         *   声明的 `Int?` 不一致（虽然 KudosContext.dataSourceId 是 String? 所以没炸）
         * - 注释里 `USERNAME_KEY = headers.get(USERNAME_KEY)` 是非法语法占位
         *
         * 当前无任何 callsite——属于"未被使用、但被使用时会出错"的预埋 bug。修复并保留以备
         * consumer 侧需要还原 context 时使用。
         */
        fun toContextParam(headers: MessageHeaders): KudosContext {
            return KudosContext().apply {
                tenantId = headers[TENANT_ID_KEY] as String?
                dataSourceId = headers[DATA_SOURCE_ID_KEY] as String?
                _datasourceTenantId = headers[DATASOURCE_TENANT_ID] as String?
            }
        }
    }
}