package io.kudos.ms.msg.common.template.vo

import java.io.Serializable


/**
 * A "sendable" message produced after template rendering.
 *
 * Relation to [MsgTemplateCacheEntry]: the template's title/content are the source with `${param}`
 * placeholders; this VO is the final product with placeholders substituted, ready to be embedded in
 * an SMTP / SMS / push payload.
 *
 * @author K
 * @since 1.0.0
 */
data class RenderedMessage(

    /** Rendered title; an empty string when both the template title and the default title are blank. */
    val title: String,

    /** Rendered body; an empty string when both the template body and the default body are blank. */
    val content: String,

    /** Map of parameters actually used during rendering, useful for tracing/auditing which placeholders were substituted. */
    val paramsUsed: Map<String, String>,

) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}
