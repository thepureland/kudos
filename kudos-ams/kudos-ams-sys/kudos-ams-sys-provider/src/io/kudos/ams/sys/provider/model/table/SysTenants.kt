package io.kudos.ams.sys.provider.model.table

import io.kudos.ams.sys.provider.model.po.SysTenant
import org.ktorm.schema.*
import io.kudos.ability.data.rdb.ktorm.support.MaintainableTable


/**
 * 租户数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysTenants : MaintainableTable<SysTenant>("sys_tenant") {
//endregion your codes 1

    /** 名称 */
    var name = varchar("name").bindTo { it.name }

    /** 时区 */
    var timezone = varchar("timezone").bindTo { it.timezone }

    /** 默认语言编码 */
    var defaultLanguageCode = varchar("default_language_code").bindTo { it.defaultLanguageCode }


    //region your codes 2

    //endregion your codes 2

}