package io.kudos.ms.sys.core.outline.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.outline.model.po.SysOutLine
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * 出网白名单数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
object SysOutLines : ManagedTable<SysOutLine>("sys_out_line") {

    /** 名称 */
    var name = varchar("name").bindTo { it.name }

    /** 主机名或通配符 */
    var host = varchar("host").bindTo { it.host }

    /** 端口；NULL 表示任意端口 */
    var port = int("port").bindTo { it.port }

    /** 协议 */
    var protocol = varchar("protocol").bindTo { it.protocol }

    /** 系统编码 */
    var systemCode = varchar("system_code").bindTo { it.systemCode }

    /** 租户id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

}
