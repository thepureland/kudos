package io.kudos.ms.sys.common.vo.dict

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 字典缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictCacheEntry (

    /** 主键 */
    override val id: String = "",


    /** 字典类型 */
    val dictType: String = "",

    /** 字典名称 */
    val dictName: String = "",

    /** 原子服务编码 */
    val atomicServiceCode: String = "",

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = true,

) : IIdEntity<String>, Serializable {


    constructor() : this("")


    companion object {
        private const val serialVersionUID = 7553349815212490728L
    }

}