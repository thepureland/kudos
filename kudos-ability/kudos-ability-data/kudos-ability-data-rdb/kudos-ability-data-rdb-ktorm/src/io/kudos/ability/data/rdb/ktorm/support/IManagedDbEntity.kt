package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.base.model.contract.common.IActivable
import io.kudos.base.model.contract.common.IAuditable
import io.kudos.base.model.contract.common.IHasBuiltIn
import io.kudos.base.model.contract.common.IHasRemark
import org.ktorm.entity.Entity

/**
 * 带有管理字段的数据库实体接口
 *
 * @param ID 主键类型
 * @param E 实体类型
 * @author K
 * @since 1.0.0
 */
interface IManagedDbEntity<ID, E : Entity<E>> :
    IDbEntity<ID, E>,
    IActivable,
    IAuditable,
    IHasBuiltIn,
    IHasRemark