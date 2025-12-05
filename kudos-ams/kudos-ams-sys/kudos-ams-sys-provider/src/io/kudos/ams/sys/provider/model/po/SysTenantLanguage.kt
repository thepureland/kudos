package io.kudos.ams.sys.provider.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 租户-语言关系数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysTenantLanguage : IDbEntity<String, SysTenantLanguage> {
//endregion your codes 1

    companion object : DbEntityFactory<SysTenantLanguage>()

    /** 租户id */
    var tenantId: String

    /** 语言代码 */
    var languageCode: String

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