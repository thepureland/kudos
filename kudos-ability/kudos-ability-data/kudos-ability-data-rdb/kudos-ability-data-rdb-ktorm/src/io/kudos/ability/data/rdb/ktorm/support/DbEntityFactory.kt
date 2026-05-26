package io.kudos.ability.data.rdb.ktorm.support

import org.ktorm.entity.Entity

/**
 * Database entity factory.
 * Simplifies usage (companion object : Entity.Factory<E>() => companion object : DbEntityFactory<E>())
 * and keeps Ktorm-related code from leaking into the PO.
 *
 * @param E Entity type
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
abstract class  DbEntityFactory<E: Entity<E>>: Entity.Factory<E>()