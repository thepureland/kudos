package io.kudos.ms.sys.common.resource.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * Resource cache entry.
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceCacheEntry (

    /** Primary key */
    override val id: String,

    /** Name */
    val name: String?,

    /** url */
    val url: String?,

    /** Resource type dict code */
    val resourceTypeDictCode: String?,

    /** Parent id */
    val parentId: String?,

    /** Order number among siblings under the same parent */
    val orderNum: Int?,

    /** Icon */
    val icon: String?,

    /** Sub-system code */
    val subSystemCode: String?,

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
        private const val serialVersionUID = 8029707342616140104L
    }

}
