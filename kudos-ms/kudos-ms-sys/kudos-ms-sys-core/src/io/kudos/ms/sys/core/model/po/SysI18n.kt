package io.kudos.ms.sys.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IMaintainableDbEntity

/**
 * 国际化数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysI18n : IMaintainableDbEntity<String, SysI18n> {
//endregion your codes 1

    companion object : DbEntityFactory<SysI18n>()

    /** 语言_地区 */
    var locale: String

    /** 原子服务编码 */
    var atomicServiceCode: String

    /** 国际化类型字典代码 */
    var i18nTypeDictCode: String

    /** 国际化命名空间 */
    var namespace: String

    /** 国际化key */
    var key: String

    /** 国际化值 */
    var value: String

    //region your codes 2

    //endregion your codes 2

}
