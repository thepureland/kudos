package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import io.kudos.base.query.sort.Sortable
import java.time.LocalDateTime

/**
 * 测试表数据库实体
 *
 * [name]、[height] 带 [Sortable] 供 [io.kudos.ability.data.rdb.ktorm.support.BaseReadOnlyDaoTest] 校验 ListSearchPayload 排序规则；其余列未标注，用于测「未标注则忽略排序」。
 *
 * @author K
 * @since 1.0.0
 */
internal interface TestTableKtorm : IDbEntity<Int, TestTableKtorm> {

    companion object Companion : DbEntityFactory<TestTableKtorm>()

    /** 名字 */
    @get:Sortable
    var name: String

    /** 生日 */
    var birthday: LocalDateTime?

    /** 是否生效 */
    var active: Boolean?

    /** 体重 */
    var weight: Double?

    /** 身高 */
    @get:Sortable
    var height: Int?




}