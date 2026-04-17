package io.kudos.ms.sys.common.microservice.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 微服务缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceCacheEntry (

    override val id: String,

    /** 编码 */
    val code: String,

    /** 名称 */
    val name: String,

    /** 上下文 */
    val context: String,

    /** 是否为原子服务 */
    val atomicService: Boolean,

    /** 父服务编码 */
    val parentCode: String?,

    /** 备注 */
    val remark: String?,

    /** 是否启用 */
    val active: Boolean,

    /** 是否内置 */
    val builtIn: Boolean,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 3759252597026207298L
    }

}
