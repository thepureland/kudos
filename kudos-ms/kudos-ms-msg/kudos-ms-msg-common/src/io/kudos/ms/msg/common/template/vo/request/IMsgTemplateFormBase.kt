package io.kudos.ms.msg.common.template.vo.request

/**
 * Common fields for message template forms (shared by create / update).
 *
 * @author K
 * @since 1.0.0
 */
interface IMsgTemplateFormBase {

    /** Send type dict code. */
    val sendTypeDictCode: String?

    /** Event type dict code. */
    val eventTypeDictCode: String?

    /** Message type dict code. */
    val msgTypeDictCode: String?

    /** Template group code. */
    val receiverGroupCode: String?

    /** Country-language dict code. */
    val localeDictCode: String?

    /** Template title. */
    val title: String?

    /** Template content. */
    val content: String?

    /** Default active flag. */
    val defaultActive: Boolean?

    /** Default template title. */
    val defaultTitle: String?

    /** Default template content. */
    val defaultContent: String?

    /** Tenant id. */
    val tenantId: String?
}
