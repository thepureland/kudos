package io.kudos.ams.sys.provider.model.table

import io.kudos.ams.sys.provider.model.po.SysI18n
import org.ktorm.schema.*
import io.kudos.ability.data.rdb.ktorm.support.StringIdTable


/**
 * 国际化数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysI18ns : StringIdTable<SysI18n>("sys_i18n") {
//endregion your codes 1

    /** 语言_地区 */
    var locale = varchar("locale").bindTo { it.locale }

    /** 语言_地区 */
    var moduleCode = varchar("module_code").bindTo { it.moduleCode }

    /** 国际化类型字典代码 */
    var i18nTypeDictCode = varchar("i18n_type_dict_code").bindTo { it.i18nTypeDictCode }

    /** 国际化key */
    var key = varchar("key").bindTo { it.key }

    /** 国际化值 */
    var value = varchar("value").bindTo { it.value }

    /** 是否启用 */
    var active = boolean("active").bindTo { it.active }

    /** 是否内置 */
    var builtIn = boolean("built_in").bindTo { it.builtIn }

    /** 创建用户 */
    var createUser = varchar("create_user").bindTo { it.createUser }

    /** 创建时间 */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** 更新用户 */
    var updateUser = varchar("update_user").bindTo { it.updateUser }

    /** 更新时间 */
    var updateTime = datetime("update_time").bindTo { it.updateTime }


    //region your codes 2

    //endregion your codes 2

}