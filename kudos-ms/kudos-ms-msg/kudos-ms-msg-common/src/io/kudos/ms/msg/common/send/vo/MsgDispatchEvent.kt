package io.kudos.ms.msg.common.send.vo

import java.io.Serializable


/**
 * Event payload dispatched to a single channel listener.
 *
 * Design trade-offs:
 * - **No templateId**: the publish service has already rendered the template; the listener only sees
 *   [renderedTitle] / [renderedContent]. This keeps in-flight messages unaffected by template edits
 *   and provides stable debugging traces.
 * - **receiverIds delivered together**: the current implementation dispatches one event per publish,
 *   bundling all recipients; channel listeners decide whether to internally paginate
 *   (Email defaults to a single SMTP batch; Site persists per user).
 * - **tenantId is required**: listeners need it for tenant-scoped credential lookup and rate-limit isolation.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgDispatchEvent(

    /** [io.kudos.ms.msg.core.send.model.po.MsgSend] primary key, used to write back counts and final status. */
    val sendId: String,

    /** [io.kudos.ms.msg.core.instance.model.po.MsgInstance] primary key, used for audit. */
    val instanceId: String,

    /** Send channel dict code: [io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum.dictCode]. */
    val publishMethodDictCode: String,

    /** Set of recipient user ids -- user-level dispatch only; tenant/role dispatch must be expanded at publish time. */
    val receiverIds: Set<String>,

    /** Rendered title with placeholders substituted. */
    val renderedTitle: String,

    /** Rendered body with placeholders substituted. */
    val renderedContent: String,

    /** Locale dict code; listeners can use it to pick the locale variant of an SMTP/SMS template. */
    val localeDictCode: String?,

    /** Tenant id (required). */
    val tenantId: String,

) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}
