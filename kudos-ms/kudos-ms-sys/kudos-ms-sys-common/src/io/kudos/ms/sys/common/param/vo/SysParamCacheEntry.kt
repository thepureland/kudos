package io.kudos.ms.sys.common.param.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * Cache entry for parameters.
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamCacheEntry (

    /** Primary key */
    override val id: String,

    /** Parameter name */
    val paramName: String,

    /** Parameter value */
    val paramValue: String,

    /** Default parameter value */
    val defaultValue: String?,

    /** Atomic service code */
    val atomicServiceCode: String,

    /** Order number */
    val orderNum: Int?,

    /** Remark */
    val remark: String?,

    /** Whether enabled */
    val active: Boolean,

    /** Whether built-in */
    val builtIn: Boolean,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 4541811200495435621L
    }

}
