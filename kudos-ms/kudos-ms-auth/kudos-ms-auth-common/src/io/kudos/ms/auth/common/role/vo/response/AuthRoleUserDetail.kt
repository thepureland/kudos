package io.kudos.ms.auth.common.role.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 角色-用户关系详情响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class AuthRoleUserDetail (

    /** 主键 */
    override val id: String = "",

    /** 角色id */
    val roleId: String? = null,

    /** 用户id */
    val userId: String? = null,

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