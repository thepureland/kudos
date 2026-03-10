package io.kudos.ms.sys.common.vo.accessrule

import io.kudos.base.support.IIdEntity
import java.io.Serializable


/**
 * 访问规则缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleCacheEntry (

    /** 主键 */
    override val id: String = "",

    //region your codes 1

    /** 租户id */
    val tenantId: String = "",

    /** 系统编码 */
    val systemCode: String = "",

    /** 规则类型 */
    val ruleType: Int = 0,

    //endregion your codes 1
//region your codes 2
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

    companion object {
        private const val serialVersionUID = 8253788046293050901L
    }

}