package io.kudos.ms.sys.common.resource.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Resource detail response VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceDetail (

    /** Primary key */
    override val id: String = "",


    /** Name */
    val name: String = "",

    /** URL */
    val url: String? = null,

    /** Resource type dictionary code */
    val resourceTypeDictCode: String = "",

    /** Parent id */
    val parentId: String? = null,

    /** Order number among siblings under the same parent */
    val orderNum: Int? = null,

    /** Icon */
    val icon: String? = null,

    /** Subsystem code */
    val subSystemCode: String = "",

    /** Remark */
    val remark: String? = null,

    /** Whether enabled */
    val active: Boolean = true,

    /** Whether built-in */
    val builtIn: Boolean = false,

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

) : IIdEntity<String> {


    /** All parent IDs */
    var parentIds: List<String>? = null

}