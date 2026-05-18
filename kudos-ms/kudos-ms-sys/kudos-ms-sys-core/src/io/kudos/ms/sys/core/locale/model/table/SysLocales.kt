package io.kudos.ms.sys.core.locale.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.locale.model.po.SysLocale
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * 语言/区域字典数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
object SysLocales : ManagedTable<SysLocale>("sys_locale") {

    /** 语言代码 */
    var code = varchar("code").bindTo { it.code }

    /** 显示名称 */
    var displayName = varchar("display_name").bindTo { it.displayName }

    /** 英文名称 */
    var englishName = varchar("english_name").bindTo { it.englishName }

    /** 排序号 */
    var sortNo = int("sort_no").bindTo { it.sortNo }

}
