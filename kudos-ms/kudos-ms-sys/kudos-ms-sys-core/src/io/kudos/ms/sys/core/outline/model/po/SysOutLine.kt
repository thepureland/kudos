package io.kudos.ms.sys.core.outline.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable


/**
 * 出网白名单数据库实体
 *
 * @author K
 * @since 1.0.0
 */
interface SysOutLine : IManagedDbEntity<String, SysOutLine> {

    companion object : DbEntityFactory<SysOutLine>()

    /** 名称 */
    @get:Sortable
    var name: String

    /** 主机名或通配符(如 *.example.com) */
    var host: String

    /** 端口；`null` 表示任意端口 */
    var port: Int?

    /** 协议(http/https/tcp/any) */
    var protocol: String

    /** 系统编码 */
    var systemCode: String

    /** 租户id；`null` 表示平台级 */
    var tenantId: String?

}
