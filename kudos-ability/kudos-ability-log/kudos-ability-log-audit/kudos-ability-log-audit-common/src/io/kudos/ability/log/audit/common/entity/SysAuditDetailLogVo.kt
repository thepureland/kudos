package io.kudos.ability.log.audit.common.entity

import java.io.Serial
import java.io.Serializable

/**
 * 审计明细 VO。
 *
 * 一条审计日志可包含多条明细（例如批量操作每个实体一条），明细持有具体的请求 URL、参数、描述信息，
 * 而 [SysAuditLogVo] 描述的是整次操作的元数据。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class SysAuditDetailLogVo : Serializable {
    /**
     * 主键
     */
    var id: String? = null

    /**
     * 业务实体id(被操作对象id)
     */
    var auditId: String? = null

    /**
     * 操作URL(完整路径)
     */
    var operateUrl: String? = null

    /**
     * 描述参数,对应:{0}
     */
    var stringParams: String? = null

    /**
     * 描述参数,JSON串,对应:${}
     */
    var objectParams: String? = null

    /**
     * requestReferer
     */
    var requestReferer: String? = null

    /**
     * POST请求数据
     */
    var requestFormData: String? = null

    /**
     * 详情描述，对应post数据的转换
     */
    var description: String? = null

    /** 默认无参构造，反序列化用 */
    constructor()

    /**
     * 带主键的构造（用于查询返回时手工组装明细）。
     *
     * @param id 明细主键
     * @author K
     * @since 1.0.0
     */
    constructor(id: String?) {
        this.id = id
    }

    companion object {
        /** 描述字段的临时占位 key，业务上下文里发现该值时表示该明细描述尚未格式化 */
        const val AUDIT_LOG_DESC: String = "__AUDIT_LOG_TMP_DESC__"

        /** Serializable 版本号 */
        @Serial
        private val serialVersionUID = -3787813089272077741L
    }
}
