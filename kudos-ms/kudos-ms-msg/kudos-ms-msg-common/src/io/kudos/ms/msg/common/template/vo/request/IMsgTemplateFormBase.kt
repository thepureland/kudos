package io.kudos.ms.msg.common.template.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/**
 * Common fields for message template forms (shared by create / update).
 *
 * Max-length constraints mirror the `msg_template` DDL column widths; [content] / [defaultContent]
 * are unbounded (`varchar` / CLOB) in the schema, so they are capped at [CONTENT_MAX_LENGTH] here to
 * keep an oversized template from being persisted and later blowing up rendering (OOM). Constraints
 * live on the interface getters so both the create and update forms inherit them; they are enforced
 * by the `@Valid` on BaseCrudController's save/update.
 *
 * @author K
 * @since 1.0.0
 */
interface IMsgTemplateFormBase {

    /** Send type dict code. */
    @get:MaxLength(6)
    val sendTypeDictCode: String?

    /** Event type dict code. */
    @get:MaxLength(32)
    val eventTypeDictCode: String?

    /** Message type dict code. */
    @get:MaxLength(16)
    val msgTypeDictCode: String?

    /** Template group code. */
    @get:MaxLength(36)
    val receiverGroupCode: String?

    /** Country-language dict code. */
    @get:MaxLength(5)
    val localeDictCode: String?

    /** Template title. */
    @get:MaxLength(256)
    val title: String?

    /** Template content. */
    @get:MaxLength(CONTENT_MAX_LENGTH)
    val content: String?

    /** Default active flag. */
    val defaultActive: Boolean?

    /** Default template title. */
    @get:MaxLength(256)
    val defaultTitle: String?

    /** Default template content. */
    @get:MaxLength(CONTENT_MAX_LENGTH)
    val defaultContent: String?

    /** Tenant id. */
    @get:MaxLength(36)
    val tenantId: String?

    companion object {
        /**
         * Upper bound for template body length. Matches MySQL `TEXT` (65535 bytes) — a sane guard for
         * the schema's unbounded `varchar` content columns, and protects the renderer from OOM on a
         * maliciously large template.
         */
        const val CONTENT_MAX_LENGTH = 65535
    }
}
