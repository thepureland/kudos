package io.kudos.ability.log.audit.commobn.support

import io.kudos.ability.log.audit.commobn.entity.BaseLog


@org.springframework.stereotype.Component
class DefaultAuditLogDetailDescriptionFormatter : IAuditLogDetailDescriptionFormatter {
    override fun descriptionFormat(logVo: BaseLog?): String {
        return ""
    }
}
