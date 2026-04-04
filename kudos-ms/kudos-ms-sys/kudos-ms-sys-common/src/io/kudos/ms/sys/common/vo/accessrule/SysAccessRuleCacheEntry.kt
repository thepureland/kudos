package io.kudos.ms.sys.common.vo.accessrule

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 访问规则缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleCacheEntry (

    /** 主键 */
    override val id: String,

    /** 租户id */
    val tenantId: String,

    /** 系统编码 */
    val systemCode: String,

    /** 规则类型 */
    val ruleType: Int,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 8253788046293050901L
    }

}
