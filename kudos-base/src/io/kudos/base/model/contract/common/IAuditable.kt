package io.kudos.base.model.contract.common

import java.time.LocalDateTime

/**
 * Auditable (carrying audit-related properties) model interface.
 *
 * @author K
 * @since 1.0.0
 */
interface IAuditable {

    /** Record creation time */
    var createTime: LocalDateTime?

    /** Record creator id */
    var createUserId: String?

    /** Record creator name */
    var createUserName: String?

    /** Record update time */
    var updateTime: LocalDateTime?

    /** Record updater id */
    var updateUserId: String?

    /** Record updater name */
    var updateUserName: String?

}
