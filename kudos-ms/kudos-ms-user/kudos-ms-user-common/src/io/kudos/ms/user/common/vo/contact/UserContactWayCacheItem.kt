package io.kudos.ms.user.common.vo.contact

import io.kudos.base.support.IIdEntity
import java.io.Serializable


/**
 * 用户联系方式缓存项
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class UserContactWayCacheItem (

    /** 主键 */
    override var id: String = "",

    //region your codes 1

    /** 用户ID */
    var userId: String? = null,

    /** 联系方式字典码 */
    var contactWayDictCode: String? = null,

    /** 联系方式值 */
    var contactWayValue: String? = null,

    /** 联系方式状态字典码 */
    var contactWayStatusDictCode: String? = null,

    /** 优先级 */
    var priority: Short? = null,

    /** 备注 */
    var remark: String? = null,

    //endregion your codes 1
//region your codes 2
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    //endregion your codes 3

    companion object {
        private const val serialVersionUID = 1L
    }

}
