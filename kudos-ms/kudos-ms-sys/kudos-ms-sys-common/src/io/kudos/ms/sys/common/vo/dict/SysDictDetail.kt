package io.kudos.ms.sys.common.vo.dict

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 字典查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictDetail (

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

    /** 创建者id */
    val createUserId: String? = null,

    /** 创建者名称 */
    val createUserName: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新者id */
    val updateUserId: String? = null,

    /** 更新者名称 */
    val updateUserName: String? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

) : IdJsonResult<String>() {


    constructor() : this("")


}