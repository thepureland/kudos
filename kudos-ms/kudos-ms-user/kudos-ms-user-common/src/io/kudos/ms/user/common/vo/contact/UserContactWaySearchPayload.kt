package io.kudos.ms.user.common.vo.contact

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 用户联系方式查询条件载体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class UserContactWaySearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = UserContactWayRecord::class,

    /** 用户ID */
    var userId: String? = null,

    /** 联系方式字典码 */
    var contactWayDictCode: String? = null,

    /** 联系方式值 */
    var contactWayValue: String? = null,

    /** 联系方式状态字典码 */
    var contactWayStatusDictCode: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(UserContactWayRecord::class)

    //endregion your codes 3

}
