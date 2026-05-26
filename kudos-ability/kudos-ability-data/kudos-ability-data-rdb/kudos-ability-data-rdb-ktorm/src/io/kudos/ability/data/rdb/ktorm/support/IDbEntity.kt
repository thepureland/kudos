package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.base.model.contract.entity.IMutableIdEntity
import org.ktorm.entity.Entity

/**
 * Database-table record entity interface.
 *
 * @param ID Primary key type
 * @param E Entity type
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IDbEntity<ID, E : Entity<E>>: IMutableIdEntity<ID>, Entity<E>