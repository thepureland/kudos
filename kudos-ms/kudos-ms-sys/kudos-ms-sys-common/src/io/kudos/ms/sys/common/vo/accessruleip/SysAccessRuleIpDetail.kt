package io.kudos.ms.sys.common.vo.accessruleip

import io.kudos.base.model.contract.result.IdJsonResult
import java.time.LocalDateTime


/**
 * ip访问规则查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpDetail (


    override val id: String = "",

    /** ip起 */
    val ipStart: Long? = null,

    /** ip止 */
    val ipEnd: Long? = null,

    /** ip类型 */
    val ipType: Int? = null,

    /** 过期时间 */
    val expirationDate: LocalDateTime? = null,

    /** 父规则id */
    val parentRuleId: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

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