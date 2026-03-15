package io.kudos.ms.sys.core.model.table

import io.kudos.ability.data.rdb.ktorm.support.MaintainableTable
import io.kudos.ms.sys.core.model.po.SysI18n
import org.ktorm.schema.varchar


/**
 * 国际化数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
object SysI18ns : MaintainableTable<SysI18n>("sys_i18n") {

    /** 语言_地区 */
    var locale = varchar("locale").bindTo { it.locale }

    /** 原子服务编码 */
    var atomicServiceCode = varchar("atomic_service_code").bindTo { it.atomicServiceCode }

    /** 国际化类型字典代码 */
    var i18nTypeDictCode = varchar("i18n_type_dict_code").bindTo { it.i18nTypeDictCode }

    /** 国际化命名空间 */
    var namespace = varchar("namespace").bindTo { it.namespace }

    /** 国际化key */
    var key = varchar("key").bindTo { it.key }

    /** 国际化值 */
    var value = varchar("value").bindTo { it.value }



}
