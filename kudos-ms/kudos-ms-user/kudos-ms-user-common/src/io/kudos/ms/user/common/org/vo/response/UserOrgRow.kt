package io.kudos.ms.user.common.org.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Organization list query result response VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserOrgRow (

    /** Primary key */
    override val id: String = "",

    /** Organization name */
    val name: String? = null,

    /** Organization short name */
    val shortName: String? = null,

    /** Tenant id */
    val tenantId: String? = null,

    /** Parent organization id */
    val parentId: String? = null,

    /** Organization type dict code */
    val orgTypeDictCode: String? = null,

    /** Sort number */
    val sortNum: Int? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether active */
    val active: Boolean? = null,

    /** Whether built-in */
    val builtIn: Boolean? = null,

    /** Creator id */
    val createUserId: String? = null,

    /** Creator name */
    val createUserName: String? = null,

    /** Create time */
    val createTime: LocalDateTime? = null,

    /** Updater id */
    val updateUserId: String? = null,

    /** Updater name */
    val updateUserName: String? = null,

    /** Update time */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>
