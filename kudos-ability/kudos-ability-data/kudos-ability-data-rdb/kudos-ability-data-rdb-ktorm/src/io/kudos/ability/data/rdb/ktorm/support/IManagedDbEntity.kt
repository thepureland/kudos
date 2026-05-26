package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.base.model.contract.common.IActivable
import io.kudos.base.model.contract.common.IAuditable
import io.kudos.base.model.contract.common.IHasBuiltIn
import io.kudos.base.model.contract.common.IHasRemark
import org.ktorm.entity.Entity

/**
 * Database entity interface that includes management fields.
 *
 * @param ID Primary key type
 * @param E Entity type
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IManagedDbEntity<ID, E : Entity<E>> :
    IDbEntity<ID, E>,
    IActivable,
    IAuditable,
    IHasBuiltIn,
    IHasRemark