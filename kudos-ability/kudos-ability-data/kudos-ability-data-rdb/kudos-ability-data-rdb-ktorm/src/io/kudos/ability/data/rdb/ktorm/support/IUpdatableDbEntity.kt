package io.kudos.ability.data.rdb.ktorm.support

import org.ktorm.entity.Entity
import java.time.LocalDateTime

/**
 * 可更新的数据库记录的实体接口
 *
 * @param ID 主键类型
 * @param E 实体类型
 * @author K
 * @since 1.0.0
 */
interface IUpdatableDbEntity<ID, E : Entity<E>> : IDbEntity<ID, E> {

    /** 记录创建时间 */
    var createTime: LocalDateTime?

    /** 记录创建者id */
    var createUserId: String?

    /** 记录创建者名称 */
    var createUserName: String?

    /** 记录更新时间 */
    var updateTime: LocalDateTime?

    /** 记录更新者id */
    var updateUserId: String?

    /** 记录更新者名称 */
    var updateUserName: String?

    /** 是否内置 */
    var builtIn: Boolean

    /** 备注 */
    var remark: String?

}