package io.kudos.ms.user.common.contact.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 用户联系方式缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class UserContactWayCacheEntry (

    /** 主键 */
    override val id: String,

    /** 用户ID */
    val userId: String?,

    /** 联系方式字典码 */
    val contactWayDictCode: String?,

    /** 联系方式值 */
    val contactWayValue: String?,

    /** 联系方式状态字典码 */
    val contactWayStatusDictCode: String?,

    /** 优先级 */
    val priority: Short?,

    /** 备注 */
    val remark: String?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

}
