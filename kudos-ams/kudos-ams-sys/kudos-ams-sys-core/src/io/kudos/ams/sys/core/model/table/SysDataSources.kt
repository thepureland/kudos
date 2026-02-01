package io.kudos.ams.sys.core.model.table

import io.kudos.ams.sys.core.model.po.SysDataSource
import org.ktorm.schema.*
import io.kudos.ability.data.rdb.ktorm.support.MaintainableTable


/**
 * 数据源数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysDataSources : MaintainableTable<SysDataSource>("sys_data_source") {
//endregion your codes 1

    /** 名称 */
    var name = varchar("name").bindTo { it.name }

    /** 子系统编码 */
    var subSystemCode = varchar("sub_system_code").bindTo { it.subSystemCode }

    /** 微服务编码 */
    var microServiceCode = varchar("micro_service_code").bindTo { it.microServiceCode }

    /** 原子服务编码 */
    var atomicServiceCode = varchar("atomic_service_code").bindTo { it.atomicServiceCode }

    /** 租户id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** url */
    var url = varchar("url").bindTo { it.url }

    /** 用户名 */
    var username = varchar("username").bindTo { it.username }

    /** 密码 */
    var password = varchar("password").bindTo { it.password }

    /** 初始连接数。初始化发生在显示调用init方法，或者第一次getConnection时 */
    var initialSize = int("initial_size").bindTo { it.initialSize }

    /** 最大连接数 */
    var maxActive = int("max_active").bindTo { it.maxActive }

    /** 最大空闲连接数 */
    var maxIdle = int("max_idle").bindTo { it.maxIdle }

    /** 最小空闲连接数。至少维持多少个空闲连接 */
    var minIdle = int("min_idle").bindTo { it.minIdle }

    /** 出借最长期限(毫秒)。客户端从连接池获取（借出）一个连接后，超时没有归还（return），则连接池会抛出异常 */
    var maxWait = int("max_wait").bindTo { it.maxWait }

    /** 连接寿命(毫秒)。超时(相对于初始化时间)连接池将在出借或归还时删除这个连接 */
    var maxAge = int("max_age").bindTo { it.maxAge }


    //region your codes 2

    //endregion your codes 2

}