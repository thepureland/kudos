package io.kudos.ms.user.common.vo.contact

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 用户联系方式缓存项
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class UserContactWayCacheEntry (

    /** 主键 */
    override val id: String = "",


    /** 用户ID */
    val userId: String? = null,

    /** 联系方式字典码 */
    val contactWayDictCode: String? = null,

    /** 联系方式值 */
    val contactWayValue: String? = null,

    /** 联系方式状态字典码 */
    val contactWayStatusDictCode: String? = null,

    /** 优先级 */
    val priority: Short? = null,

    /** 备注 */
    val remark: String? = null,

) : IIdEntity<String>, Serializable {


    constructor() : this("")


    companion object {
        private const val serialVersionUID = 1L
    }

}
