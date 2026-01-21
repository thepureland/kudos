package io.kudos.ams.sys.provider.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 微服务-原子服务关系数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysMicroServiceAtomicService : IDbEntity<String, SysMicroServiceAtomicService> {
//endregion your codes 1

    companion object : DbEntityFactory<SysMicroServiceAtomicService>()

    /** 微服务编码 */
    var microServiceCode: String

    /** 原子服务编码 */
    var atomicServiceCode: String

    /** 创建者id */
    var createUserId: String?

    /** 创建者名称 */
    var createUserName: String?

    /** 创建时间 */
    var createTime: LocalDateTime?

    /** 更新者id */
    var updateUserId: String?

    /** 更新者名称 */
    var updateUserName: String?

    /** 更新时间 */
    var updateTime: LocalDateTime?


    //region your codes 2

    //endregion your codes 2

}