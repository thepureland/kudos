package io.kudos.ms.user.common.org.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * Organization cache entry
 *
 * @author K
 * @since 1.0.0
 */
data class UserOrgCacheEntry (

    /** Primary key */
    override val id: String,

    /** Organization name */
    val name: String?,

    /** Organization short name */
    val shortName: String?,

    /** Tenant id */
    val tenantId: String?,

    /** Parent organization id */
    val parentId: String?,

    /** Organization type dict code */
    val orgTypeDictCode: String?,

    /** Sort number */
    val sortNum: Int?,

    /** Remark */
    val remark: String?,

    /** Whether active */
    val active: Boolean?,

    /** Whether built-in */
    val builtIn: Boolean?,

    /** Creator id */
    val createUserId: String?,

    /** Creator name */
    val createUserName: String?,

    /** Create time */
    val createTime: LocalDateTime?,

    /** Updater id */
    val updateUserId: String?,

    /** Updater name */
    val updateUserName: String?,

    /** Update time */
    val updateTime: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

}
