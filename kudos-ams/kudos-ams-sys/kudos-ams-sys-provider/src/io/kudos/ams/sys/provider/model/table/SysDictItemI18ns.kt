package io.kudos.ams.sys.provider.model.table

import io.kudos.ams.sys.provider.model.po.SysDictItemI18n
import org.ktorm.schema.*
import io.kudos.ability.data.rdb.ktorm.support.StringIdTable


/**
 * 字典项国际化数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysDictItemI18ns : StringIdTable<SysDictItemI18n>("sys_dict_item_i18n") {
//endregion your codes 1

    /** 语言_地区 */
    var locale = varchar("locale").bindTo { it.locale }

    /** 国际化值 */
    var i18nValue = varchar("i18n_value").bindTo { it.i18nValue }

    /** 字典项id */
    var itemId = varchar("item_id").bindTo { it.itemId }

    /** 是否启用 */
    var active = boolean("active").bindTo { it.active }

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