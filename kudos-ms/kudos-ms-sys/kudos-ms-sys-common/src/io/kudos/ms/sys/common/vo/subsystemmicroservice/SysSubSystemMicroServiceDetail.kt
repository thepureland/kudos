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
    override var id: String? = null,

    //region your codes 1

    /** 子系统编码 */
    var subSystemCode: String? = null,

    /** 微服务编码 */
    var microServiceCode: String? = null,

    /** 创建者id */
    var createUserId: String? = null,

    /** 创建者名称 */
    var createUserName: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新者id */
    var updateUserId: String? = null,

    /** 更新者名称 */
    var updateUserName: String? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    //endregion your codes 3

}