package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.base.model.contract.entity.IMutableIdEntity
import org.ktorm.entity.Entity

/**
 * 数据库表记录实体接口
 *
 * @param ID 主键类型
 * @param E 实体类型
 * @author K
 * @since 1.0.0
 */
interface IDbEntity<ID, E : Entity<E>>: IMutableIdEntity<ID>, Entity<E>