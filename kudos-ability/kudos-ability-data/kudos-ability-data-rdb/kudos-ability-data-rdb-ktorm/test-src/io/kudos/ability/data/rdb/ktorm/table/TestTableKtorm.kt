package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import io.kudos.base.query.sort.Sortable
import java.time.LocalDateTime

/**
 * Database entity for the test table.
 *
 * [name] and [height] carry [Sortable] so [io.kudos.ability.data.rdb.ktorm.support.BaseReadOnlyDaoTest] can verify
 * the ListSearchPayload sort rules; the remaining columns are unannotated, used to test "unannotated = sort ignored".
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal interface TestTableKtorm : IDbEntity<Int, TestTableKtorm> {

    companion object Companion : DbEntityFactory<TestTableKtorm>()

    /** Name */
    @get:Sortable
    var name: String

    /** Birthday */
    var birthday: LocalDateTime?

    /** Whether active */
    var active: Boolean?

    /** Weight */
    var weight: Double?

    /** Height */
    @get:Sortable
    var height: Int?




}