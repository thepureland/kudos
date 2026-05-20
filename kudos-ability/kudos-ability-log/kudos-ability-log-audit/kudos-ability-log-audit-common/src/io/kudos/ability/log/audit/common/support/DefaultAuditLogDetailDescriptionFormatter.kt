package io.kudos.ability.log.audit.common.support

import io.kudos.ability.log.audit.common.entity.BaseLog
import org.springframework.stereotype.Component


/**
 * [IAuditLogDetailDescriptionFormatter] 的默认实现：始终返回空串。
 *
 * 业务侧无显式 formatter 时由本类兜底，保证 [io.kudos.ability.log.audit.common.annotation.Audit.descriptionFormatter]
 * 默认值有意义。需要个性化描述的业务请自行实现 [IAuditLogDetailDescriptionFormatter] 并在 [io.kudos.ability.log.audit.common.annotation.Audit]
 * 注解中指定。
 *
 * @author K
 * @since 1.0.0
 */
@Component
class DefaultAuditLogDetailDescriptionFormatter : IAuditLogDetailDescriptionFormatter {

    /**
     * 默认实现：永远返回空字符串，等价于"不生成描述"。
     *
     * @param baseLog 审计日志主体
     * @return 空串
     * @author K
     * @since 1.0.0
     */
    override fun descriptionFormat(baseLog: BaseLog?): String {
        return ""
    }

}
