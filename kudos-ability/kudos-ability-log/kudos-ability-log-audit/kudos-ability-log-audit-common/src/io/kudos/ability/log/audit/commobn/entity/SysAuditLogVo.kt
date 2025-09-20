package io.kudos.ability.log.audit.commobn.entity

import java.io.Serial
import java.io.Serializable
import java.util.*

/**
 * 系统审计日志表实体
 *
 * @author admin
 * @tableAuthor admin
 * @time 2016-9-3 16:07:08
 */
//region your codes 1
class SysAuditLogVo  //endregion
//region constuctors
    : Serializable {
    //region properties
    //endregion
    /**
     * 主键
     */
    var id: String? = null

    /**
     * 业务实体id(被操作对象id)
     */
    var entityId: String? = null

    /**
     * 操作类型(ID)
     */
    var operateTypeId: Int? = null

    /**
     * 操作类型
     */
    var operateType: String? = null

    /**
     * 模块名(多层级)
     */
    var moduleName: String? = null

    /**
     * 模型类型
     */
    var moduleCode: String? = null

    /**
     * 操作描述
     */
    var description: String? = null

    /**
     * 操作员
     */
    var operator: String? = null

    /**
     * 租户ID
     */
    var tenantId: String? = null
    var sourceTenantId: String? = null

    /**
     * 子系统code
     */
    var subSysCode: String? = null

    /**
     * 操作时间
     */
    var operateTime: Date? = null

    /**
     * 操作(客户端)IP
     */
    var operateIp: Long? = null

    /**
     * 操作者IP地区字典代码
     */
    var operateIpDictCode: String? = null

    /**
     * 操作员id
     */
    var operatorId: String? = null

    /**
     * 用户类型(参观:sys_user)
     */
    var operatorUserType: String? = null

    /**
     * 客户端操作系统
     */
    var clientOs: String? = null

    /**
     * 客户端浏览器
     */
    var clientBrowser: String? = null

    /**
     * 请求类型（GET|POST）
     */
    var requestType: String? = null
    var moduleId: Int? = null

    companion object {
        @Serial
        private const val serialVersionUID = 2339633147120186063L

        const val AUDIT_LOG: String = "__AUDIT_LOG_TMP__"
    }
}
