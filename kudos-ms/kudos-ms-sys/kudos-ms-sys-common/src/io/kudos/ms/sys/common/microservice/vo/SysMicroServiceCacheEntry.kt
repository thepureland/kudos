package io.kudos.ms.sys.common.microservice.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * Cache entry for microservices.
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceCacheEntry (

    override val id: String,

    /** Code */
    val code: String,

    /** Name */
    val name: String,

    /** Context */
    val context: String,

    /** Whether atomic service */
    val atomicService: Boolean,

    /** Parent service code */
    val parentCode: String?,

    /** Remark */
    val remark: String?,

    /** Whether enabled */
    val active: Boolean,

    /** Whether built-in */
    val builtIn: Boolean,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 3759252597026207298L
    }

}
