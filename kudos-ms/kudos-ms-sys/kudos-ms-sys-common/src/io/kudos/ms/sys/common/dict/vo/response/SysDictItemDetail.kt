package io.kudos.ms.sys.common.dict.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Dictionary item detail response VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemDetail (

    /** Primary key */
    override val id: String = "",


    /** Dictionary item code */
    val itemCode: String = "",

    /** Dictionary item name */
    val itemName: String = "",

    /** Dictionary id */
    val dictId: String = "",

    /** Dictionary item order */
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

    /** Create time */
    val createTime: LocalDateTime? = null,

    /** Updater id */
    val updateUserId: String? = null,

    /** Updater name */
    val updateUserName: String? = null,

    /** Update time */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>