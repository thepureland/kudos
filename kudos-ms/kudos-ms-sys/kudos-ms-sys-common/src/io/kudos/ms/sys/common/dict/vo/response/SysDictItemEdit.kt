package io.kudos.ms.sys.common.dict.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Response VO for dict item editing.
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemEdit (

    /** Primary key */
    override val id: String = "",


    /** Dict item code */
    val itemCode: String = "",

    /** Dict item name */
    val itemName: String = "",

    /** Dict id */
    val dictId: String = "",

    /** Dict item order */
    val orderNum: Int? = null,

    /** Parent id */
    val parentId: String? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether enabled */
    val active: Boolean = true,

    /** Whether built-in */
    val builtIn: Boolean = true,

    /** Creator id */
    val createUserId: String? = null,

    /** Creator name */
    val createUserName: String? = null,

    /** Created time */
    val createTime: LocalDateTime? = null,

    /** Updater id */
    val updateUserId: String? = null,

    /** Updater name */
    val updateUserName: String? = null,

    /** Updated time */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>
