package io.kudos.ability.log.audit.common.support

import io.kudos.ability.log.audit.common.entity.BaseLog
import org.springframework.stereotype.Component


@Component
class DefaultAuditLogDetailDescriptionFormatter : IAuditLogDetailDescriptionFormatter {

    override fun descriptionFormat(baseLog: BaseLog?): String {
        return ""
    }

}
