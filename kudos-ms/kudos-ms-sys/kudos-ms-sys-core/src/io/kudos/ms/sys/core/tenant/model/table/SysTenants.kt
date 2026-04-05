package io.kudos.ms.sys.core.tenant.model.table
import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.tenant.model.po.SysTenant
import org.ktorm.schema.varchar


/**
 * 租户数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
object SysTenants : ManagedTable<SysTenant>("sys_tenant") {

    /** 名称 */
    var name = varchar("name").bindTo { it.name }

    /** 时区 */
    var timezone = varchar("timezone").bindTo { it.timezone }

    /** 默认语言编码 */
    var defaultLanguageCode = varchar("default_language_code").bindTo { it.defaultLanguageCode }




}