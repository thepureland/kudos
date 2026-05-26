package io.kudos.ms.msg.common.send.vo.request

import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum


/**
 * Request payload for callers of [io.kudos.ms.msg.common.send.api.IMsgSendApi.publish].
 *
 * Routing model: the caller passes (tenantId, eventTypeDictCode, msgTypeDictCode, locale?);
 * the publish service uses this four-tuple to match a template. Missing template is treated as a missing
 * configuration -- nothing is sent and null is returned.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgPublishRequest(

    /** Tenant id. */
    val tenantId: String,

    /** Event type dict code (e.g. user_registered / order_paid / password_reset). */
    val eventTypeDictCode: String,

    /** Message type dict code (business-defined). */
    val msgTypeDictCode: String,

    /** Send channel -- one channel per publish call; multi-channel requires the caller to publish each separately. */
    val publishMethod: MsgPublishMethodEnum,

    /** Set of recipient user ids. */
    val receiverIds: Set<String>,

    /**
     * Business parameters to substitute into the template's `${name}` placeholders.
     * Business parameters win over auto parameters (time/date/year/...) when names collide.
     */
    val params: Map<String, String> = emptyMap(),

    /** Locale; null leaves it unconstrained and lets the publish service pick the first match. */
    val localeDictCode: String? = null,
)
