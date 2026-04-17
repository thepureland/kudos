package io.kudos.ms.sys.core.microservice.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import io.kudos.base.query.sort.Sortable
import java.time.LocalDateTime

/**
 * 微服务数据库实体
 *
 * @author K
 * @since 1.0.0
 */
interface SysMicroService : IDbEntity<String, SysMicroService> {

    companion object : DbEntityFactory<SysMicroService>()

    /** 编码 */
    @get:Sortable
    var code: String

    /** 名称 */
    @get:Sortable
    var name: String

    /** 上下文 */
    var context: String

    /** 是否为原子服务 */
    var atomicService: Boolean

    /** 父服务编码 */
    var parentCode: String?

    /** 备注 */
    var remark: String?

    /** 是否启用 */
    var active: Boolean

    /** 是否内置 */
    var builtIn: Boolean?

    /** 创建者id */
    var createUserId: String?

    /** 创建者名称 */
    var createUserName: String?

    /** 创建时间 */
    var createTime: LocalDateTime?

    /** 更新者id */
    var updateUserId: String?

    /** 更新者名称 */
    var updateUserName: String?

    /** 更新时间 */
    var updateTime: LocalDateTime?


    override var id: String
        get() = this.code
        set(value) { this.code = value }

}
