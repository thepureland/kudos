package io.kudos.ability.log.audit.mq

import io.kudos.ability.log.audit.common.annotation.Audit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.base.model.contract.entity.IIdEntity
import org.springframework.stereotype.Component

/**
 * 审计 MQ 测试用业务服务。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class TestMqAuditService {

    @Audit(opType = OperationTypeEnum.CREATE, moduleCode = "AAA：sys_module表的code")
    open fun saveLog() {
        println(1111)
    }

    open fun load(code: String?): String {
        return "code-load-success"
    }

    /**
     * 测试用保存模型。
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    class SaveModel(override var id: String) : IIdEntity<String> {
        @JvmField
        var code: String? = null
    }

}
