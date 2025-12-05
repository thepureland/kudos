package io.kudos.ams.sys.provider.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IMaintainableDbEntity

/**
 * 租户数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysTenant : IMaintainableDbEntity<String, SysTenant> {
//endregion your codes 1

    companion object : DbEntityFactory<SysTenant>()

    /** 名称 */
    var name: String

    /** 时区 */
    var timezone: String?

    /** 默认语言编码 */
    var defaultLanguageCode: String?


    //region your codes 2

    //endregion your codes 2

}