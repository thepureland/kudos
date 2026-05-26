package io.kudos.ms.msg.core.template.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity


/**
 * Message template database entity.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface MsgTemplate : IDbEntity<String, MsgTemplate> {

    companion object : DbEntityFactory<MsgTemplate>()

    /** Send type dictionary code */
    var sendTypeDictCode: String

    /** Event type dictionary code */
    var eventTypeDictCode: String

    /** Message type dictionary code */
    var msgTypeDictCode: String

    /** Template group code */
    var receiverGroupCode: String?

    /** Country-language dictionary code */
    var localeDictCode: String?

    /** Template title */
    var title: String?

    /** Template content */
    var content: String?

    /** Whether default values are enabled */
    var defaultActive: Boolean

    /** Default template title */
    var defaultTitle: String?

    /** Default template content */
    var defaultContent: String?

    /** Tenant ID */
    var tenantId: String




}
