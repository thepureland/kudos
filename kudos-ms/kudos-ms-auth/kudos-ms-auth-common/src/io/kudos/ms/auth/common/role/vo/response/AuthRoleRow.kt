package io.kudos.ms.auth.common.role.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 角色列表查询结果响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class AuthRoleRow (

    /** 主键 */
    override val id: String = "",

    /** 角色编码 */
    val code: String? = null,

    /** 角色名称 */
    val name: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 子系统编码 */
    val subsysCode: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否激活 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

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

) : IIdEntity<String>