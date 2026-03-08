package io.kudos.ms.sys.common.vo.subsystemmicroservice

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 子系统-微服务关系查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysSubSystemMicroServiceDetail (

    /**  */
    override val id: String = "",

    //region your codes 1

    /** 子系统编码 */
    val subSystemCode: String? = null,

    /** 微服务编码 */
    val microServiceCode: String? = null,

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

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    //endregion your codes 3

}