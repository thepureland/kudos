package io.kudos.ability.log.audit.mq

import io.kudos.ability.log.audit.common.annotation.Audit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.base.model.contract.entity.IIdEntity
import org.springframework.stereotype.Component

/**
 * Business service used by the audit MQ tests.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class TestMqAuditService {

    @Audit(opType = OperationTypeEnum.CREATE, moduleCode = "AAA: code of the sys_module table")
    open fun saveLog() {
        println(1111)
    }

    open fun load(code: String?): String {
        return "code-load-success"
    }

    /**
     * Save model used for testing.
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
