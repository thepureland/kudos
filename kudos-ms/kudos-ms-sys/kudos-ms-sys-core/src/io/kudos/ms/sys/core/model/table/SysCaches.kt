package io.kudos.ms.sys.core.model.table

import io.kudos.ms.sys.core.model.po.SysCache
import org.ktorm.schema.*
import io.kudos.ability.data.rdb.ktorm.support.MaintainableTable


/**
 * 缓存数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysCaches : MaintainableTable<SysCache>("sys_cache") {
//endregion your codes 1

    /** 名称 */
    var name = varchar("name").bindTo { it.name }

    /** 原子服务编码 */
    var atomicServiceCode = varchar("atomic_service_code").bindTo { it.atomicServiceCode }

    /** 缓存策略代码 */
    var strategyDictCode = varchar("strategy_dict_code").bindTo { it.strategyDictCode }

    /** 是否启动时写缓存 */
    var writeOnBoot = boolean("write_on_boot").bindTo { it.writeOnBoot }

    /** 是否及时回写缓存 */
    var writeInTime = boolean("write_in_time").bindTo { it.writeInTime }

    /** 缓存生存时间(秒) */
    var ttl = int("ttl").bindTo { it.ttl }


    //region your codes 2

    //endregion your codes 2

}