package io.kudos.ms.sys.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 子系统-微服务关系数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysSubSystemMicroService : IDbEntity<String, SysSubSystemMicroService> {
//endregion your codes 1

    companion object : DbEntityFactory<SysSubSystemMicroService>()

    /** 子系统编码 */
    var subSystemCode: String

    /** 微服务编码 */
    var microServiceCode: String

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