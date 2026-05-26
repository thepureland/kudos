package io.kudos.ms.msg.common.template.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * Message template cache entry.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgTemplateCacheEntry (

    /** Primary key. */
    override val id: String,

    /** Send type dict code. */
    val sendTypeDictCode: String?,

    /** Event type dict code. */
    val eventTypeDictCode: String?,

    /** Message type dict code. */
    val msgTypeDictCode: String?,

    /** Template group code. */
    val receiverGroupCode: String?,

    /** Country-language dict code. */
    val localeDictCode: String?,

    /** Template title. */
    val title: String?,

    /** Template content. */
    val content: String?,

    /** Default active flag. */
    val defaultActive: Boolean?,

    /** Default template title. */
    val defaultTitle: String?,

    /** Default template content. */
    val defaultContent: String?,

    /** Tenant id. */
    val tenantId: String?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 5801009370756956314L
    }

}
