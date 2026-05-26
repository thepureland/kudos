package io.kudos.ms.sys.common.param.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Parameter detail response VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamDetail (

    /** Primary key */
    override val id: String = "",


    /** Parameter name */
    val paramName: String = "",

    /** Parameter value */
    val paramValue: String = "",

    /** Default parameter value */
    val defaultValue: String? = null,

    /** Atomic service code */
    val atomicServiceCode: String = "",

    /** Order number */
    val orderNum: Int? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether active */
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