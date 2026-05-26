package io.kudos.ms.msg.core.instance.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime


/**
 * Message instance database entity.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface MsgInstance : IDbEntity<String, MsgInstance> {

    companion object : DbEntityFactory<MsgInstance>()

    /** Country-language dictionary code */
    var localeDictCode: String?

    /** Title */
    var title: String?

    /** Notification content */
    var content: String?

    /** Message template id */
    var templateId: String?

    /** Send type dictionary code */
    var sendTypeDictCode: String?

    /** Event type dictionary code */
    var eventTypeDictCode: String?

    /** Message type dictionary code */
    var msgTypeDictCode: String?

    /** Valid period start */
    var validTimeStart: LocalDateTime

    /** Valid period end */
    var validTimeEnd: LocalDateTime

    /** Tenant ID */
    var tenantId: String




}
