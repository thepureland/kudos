package io.kudos.ability.log.audit.common.support

import io.kudos.ability.log.audit.common.entity.BaseLog
import org.springframework.stereotype.Component


/**
 * Default implementation of [IAuditLogDetailDescriptionFormatter]: always returns
 * an empty string.
 *
 * Serves as the fallback when business code does not provide an explicit
 * formatter, keeping the default value of
 * [io.kudos.ability.log.audit.common.annotation.Audit.descriptionFormatter]
 * meaningful. Business code that needs a customized description should implement
 * [IAuditLogDetailDescriptionFormatter] and specify it in the
 * [io.kudos.ability.log.audit.common.annotation.Audit] annotation.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
class DefaultAuditLogDetailDescriptionFormatter : IAuditLogDetailDescriptionFormatter {

    /**
     * Default implementation: always returns an empty string, equivalent to
     * "do not generate a description".
     *
     * @param baseLog audit log body
     * @return empty string
     * @author K
     * @since 1.0.0
     */
    override fun descriptionFormat(baseLog: BaseLog?): String {
        return ""
    }

}
