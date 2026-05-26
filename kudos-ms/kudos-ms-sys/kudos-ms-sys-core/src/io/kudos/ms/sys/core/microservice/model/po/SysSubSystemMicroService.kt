package io.kudos.ms.sys.core.microservice.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Sub-system to micro-service relation DB entity.
 *
 * @author K
 * @since 1.0.0
 */
interface SysSubSystemMicroService : IDbEntity<String, SysSubSystemMicroService> {

    companion object : DbEntityFactory<SysSubSystemMicroService>()

    /** Sub-system code */
    var subSystemCode: String

    /** Micro-service code */
    var microServiceCode: String

    /** Creator id */
    var createUserId: String?

    /** Creator name */
    var createUserName: String?

    /** Create time */
    var createTime: LocalDateTime?

    /** Updater id */
    var updateUserId: String?

    /** Updater name */
    var updateUserName: String?

    /** Update time */
    var updateTime: LocalDateTime?

}