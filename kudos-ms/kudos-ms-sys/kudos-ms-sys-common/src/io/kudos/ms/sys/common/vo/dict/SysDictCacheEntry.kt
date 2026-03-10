package io.kudos.ms.sys.common.vo.dict

import io.kudos.base.support.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 字典缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictCacheEntry (

    /** 主键 */
    override val id: String = "",

    //region your codes 1

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

    //endregion your codes 1
//region your codes 2
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

    companion object {
        private const val serialVersionUID = 7553349815212490728L
    }

}