package io.kudos.ms.user.common.contact.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * User contact way cache entry
 *
 * @author K
 * @since 1.0.0
 */
data class UserContactWayCacheEntry (

    /** Primary key */
    override val id: String,

    /** User ID */
    val userId: String?,

    /** Contact way dict code */
    val contactWayDictCode: String?,

    /** Contact way value */
    val contactWayValue: String?,

    /** Contact way status dict code */
    val contactWayStatusDictCode: String?,

    /** Priority (DB column `INT2`, but Ktorm binding and PO both use Int; follows PO type to avoid ClassCastException during Integer->Short reflection construction) */
    val priority: Int?,

    /** Remark */
    val remark: String?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

}
