package io.kudos.ms.auth.core.group.model.po
import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 组-角色关系数据库实体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface AuthGroupRole : IDbEntity<String, AuthGroupRole> {

    companion object : DbEntityFactory<AuthGroupRole>()

    /** 组id */
    var groupId: String

    /** 角色id */
    var roleId: String

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




}
