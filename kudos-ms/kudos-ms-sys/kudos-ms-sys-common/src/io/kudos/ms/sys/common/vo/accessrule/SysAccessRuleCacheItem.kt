package io.kudos.ms.sys.common.vo.accessrule

import io.kudos.base.support.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 访问规则缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleCacheItem (

    /** 主键 */
    override val id: String = "",

    //region your codes 1

    /** 租户id */
    val tenantId: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    /** 规则类型 */
    val ruleType: Int? = null,

    /** 创建者id */
    val createUserId: String? = null,

    /** 创建者名称 */
    val createUserName: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新者id */
    val updateUserId: String? = null,

    /** 更新者名称 */
    val updateUserName: String? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

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