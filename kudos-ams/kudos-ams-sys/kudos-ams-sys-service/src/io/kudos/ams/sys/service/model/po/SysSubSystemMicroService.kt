package io.kudos.ams.sys.service.model.po

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

    /** 创建用户 */
    var createUser: String?

    /** 创建时间 */
    var createTime: LocalDateTime?

    /** 更新用户 */
    var updateUser: String?

    /** 更新时间 */
    var updateTime: LocalDateTime?


    //region your codes 2

    //endregion your codes 2

}