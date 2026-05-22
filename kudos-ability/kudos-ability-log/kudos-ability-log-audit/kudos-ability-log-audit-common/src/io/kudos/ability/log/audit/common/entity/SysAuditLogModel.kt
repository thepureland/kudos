package io.kudos.ability.log.audit.common.entity

import io.kudos.base.data.json.JsonKit
import java.io.Serial
import java.io.Serializable

/**
 * 审计日志聚合模型。
 *
 * 用于 MQ 投递 / 服务接口入参，把"主审计记录 ([entities])"与"对应明细 ([sysAuditDetailLogs])"打包在一起，
 * 加上跨进程透传所需的子系统和租户 id。toString 直接走 JSON 序列化以便日志排查时打印完整结构。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class SysAuditLogModel : Serializable {
    /** 审计明细列表，与 [entities] 顺序对应；批量操作时一对多 */
    var sysAuditDetailLogs: MutableList<SysAuditDetailLogVo?>? = null

    /** 主审计记录列表（每次操作可对多个实体生效） */
    var entities: MutableList<SysAuditLogVo>? = null

    /** 子系统编码，跨进程审计时用于路由到对应数据库 */
    var subSysCode: String? = null
    /** 租户 id，多租户场景下用于隔离 */
    var tenantId: String? = null

    /**
     * 直接序列化为 JSON 串，便于日志排查时打印完整结构。
     *
     * @author K
     * @since 1.0.0
     */
    override fun toString(): String {
        return JsonKit.toJson(this)
    }

    companion object {
        /** Serializable 版本号 */
        @Serial
        private val serialVersionUID = -2034863673832068399L
    }
}
