package io.kudos.ability.data.rdb.jdbc.context

import java.io.Serializable

/**
 * 数据库路由的"会话参数"，每个线程持一份，由 [DbContext] 管理。
 *
 * 切面（`DsChangeAspect` / `TenantDsChangeAspect` / `DynamicDataSourceAspect`）按此对象的
 * 字段决定本次方法调用走哪个数据源、是否只读、是否打日志。`Serializable` 是为了跨进程传递
 * （比如序列化到 MQ 消息或缓存）的预留，目前主路径不依赖。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class DbParam : Serializable {

    /** 强制指定的数据源 key。非空时切面会跳过常规路由直接切到这个 key 上。 */
    var forcedDs: String? = null

    /** 是否打路由日志（INFO 级别）。debug 路径通常不开，遇到诡异路由时手动打开排查。 */
    var enableLog: Boolean = false

    /** 是否走只读副本。某些路径（缓存预热、报表查询）开起来减轻主库压力。 */
    var readonly: Boolean = false

    /** 创建当前路由参数的快照，用于切面嵌套调用后恢复外层上下文。 */
    fun copy(): DbParam {
        return DbParam().also {
            it.forcedDs = forcedDs
            it.enableLog = enableLog
            it.readonly = readonly
        }
    }

    companion object {
        /** [Serializable] 兼容字段，避免 JDK 间反序列化失败。 */
        private const val serialVersionUID = -3788770245369263297L
    }
}
