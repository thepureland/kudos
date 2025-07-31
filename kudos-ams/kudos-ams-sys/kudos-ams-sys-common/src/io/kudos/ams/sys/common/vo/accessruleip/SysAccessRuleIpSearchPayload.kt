package io.kudos.ams.sys.common.vo.accessruleip

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass
import java.time.LocalDateTime


/**
 * ip访问规则查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = SysAccessRuleIpRecord::class,

    /** ip起 */
    var ipStart: Long? = null,

    /** ip止 */
    var ipEnd: Long? = null,

    /** ip类型 */
    var ipType: Int? = null,

    /** 过期时间 */
    var expirationDate: LocalDateTime? = null,

    /** 父规则id */
    var parentRuleId: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(SysAccessRuleIpRecord::class)

    //endregion your codes 3

}