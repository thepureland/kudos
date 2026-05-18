package io.kudos.ms.sys.core.locale.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable


/**
 * 语言/区域字典实体
 *
 * @author K
 * @since 1.0.0
 */
interface SysLocale : IManagedDbEntity<String, SysLocale> {

    companion object : DbEntityFactory<SysLocale>()

    /** 语言代码(如 zh_CN, en_US) */
    var code: String

    /** 显示名称(母语写法) */
    var displayName: String

    /** 英文名称 */
    var englishName: String

    /** 排序号 */
    @get:Sortable
    var sortNo: Int

}
