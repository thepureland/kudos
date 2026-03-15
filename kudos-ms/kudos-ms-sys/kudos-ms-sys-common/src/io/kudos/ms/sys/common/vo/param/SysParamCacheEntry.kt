package io.kudos.ms.sys.common.vo.param

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 参数缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamCacheEntry (

    /** 主键 */
    override val id: String = "",


    /** 参数名称 */
    val paramName: String = "",

    /** 参数值 */
    val paramValue: String = "",

    /** 默认参数值 */
    val defaultValue: String? = null,

    /** 原子服务编码 */
    val atomicServiceCode: String = "",

    /** 序号 */
    val orderNum: Int? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = true,

) : IIdEntity<String>, Serializable {


    constructor() : this("")


    companion object {
        private const val serialVersionUID = 4541811200495435621L
    }

}