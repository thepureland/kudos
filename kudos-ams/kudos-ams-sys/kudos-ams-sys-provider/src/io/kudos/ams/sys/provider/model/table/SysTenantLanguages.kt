package io.kudos.ams.sys.provider.model.table

import io.kudos.ams.sys.provider.model.po.SysTenantLanguage
import org.ktorm.schema.*
import io.kudos.ability.data.rdb.ktorm.support.StringIdTable


/**
 * 租户-语言关系数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysTenantLanguages : StringIdTable<SysTenantLanguage>("sys_tenant_language") {
//endregion your codes 1

    /** 租户id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** 语言代码 */
    var languageCode = varchar("language_code").bindTo { it.languageCode }

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