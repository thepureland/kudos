package io.kudos.ability.log.audit.commobn.entity

import java.io.Serial
import java.io.Serializable

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

    constructor()

    constructor(id: String?) {
        this.id = id
    }

    companion object {
        const val AUDIT_LOG_DESC: String = "__AUDIT_LOG_TMP_DESC__"

        @Serial
        private val serialVersionUID = -3787813089272077741L
    }
}
