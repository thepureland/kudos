package io.kudos.ams.sys.provider.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 模块数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysModule : IDbEntity<String, SysModule> {
//endregion your codes 1

    companion object : DbEntityFactory<SysModule>()

    /** 编码 */
    var code: String

    /** 名称 */
    var name: String

    /** 原子服务编码 */
    var atomicServiceCode: String

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


    //region your codes 2

    override var id: String?
        get() = this.code
        set(value) { this.code = value!! }

    //endregion your codes 2

}