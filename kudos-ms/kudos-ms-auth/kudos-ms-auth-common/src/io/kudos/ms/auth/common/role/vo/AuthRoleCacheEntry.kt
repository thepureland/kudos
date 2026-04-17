package io.kudos.ms.auth.common.role.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 角色缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class AuthRoleCacheEntry (

    /** 主键 */
    override val id: String,

    /** 角色编码 */
    val code: String?,

    /** 角色名称 */
    val name: String?,

    /** 租户id */
    val tenantId: String?,

    /** 子系统编码 */
    val subsysCode: String?,

    /** 备注 */
    val remark: String?,

    /** 是否激活 */
    val active: Boolean?,

    /** 是否内置 */
    val builtIn: Boolean?,

    /** 创建者id */
    val createUserId: String?,

    /** 创建者名称 */
    val createUserName: String?,

    /** 创建时间 */
    val createTime: LocalDateTime?,

    /** 更新者id */
    val updateUserId: String?,

    /** 更新者名称 */
    val updateUserName: String?,

    /** 更新时间 */
    val updateTime: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

}
