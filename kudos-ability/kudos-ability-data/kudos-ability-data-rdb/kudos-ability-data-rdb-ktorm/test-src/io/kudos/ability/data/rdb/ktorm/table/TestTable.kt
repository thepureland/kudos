package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 测试表数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
internal interface TestTable : IDbEntity<Int, TestTable> {
//endregion your codes 1

    companion object : DbEntityFactory<TestTable>()

    /** 名字 */
    var name: String

    /** 生日 */
    var birthday: LocalDateTime?

    /** 是否生效 */
    var active: Boolean?

    /** 体重 */
    var weight: Double?

    /** 身高 */
    var height: Int?


    //region your codes 2

    //endregion your codes 2

}