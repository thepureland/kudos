package io.kudos.ability.data.rdb.ktorm.rowscope

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import org.ktorm.schema.varchar
import org.springframework.stereotype.Repository


/**
 * The worked example of a row-scoped entity — what an application writes to opt a table into data
 * permissions, and the fixture `RowScopeEndToEndTest` proves the framework against.
 *
 * Two dimensions plus a creator column, because the interesting rules only show up in combination:
 * dimensions AND each other, values OR within a dimension, and SELF ORs with the whole thing.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@DataScoped(dimension = "org", column = "org_id")
@DataScoped(dimension = "region", column = "region_code")
@DataScopedSelf(column = "create_user_id")
internal interface TestScopedOrder : IDbEntity<String, TestScopedOrder> {

    companion object Companion : DbEntityFactory<TestScopedOrder>()

    var title: String
    var orgId: String?
    var regionCode: String?
    var createUserId: String?
}


internal object TestScopedOrders : StringIdTable<TestScopedOrder>("test_scoped_order") {
    var title = varchar("title").bindTo { it.title }
    var orgId = varchar("org_id").bindTo { it.orgId }
    var regionCode = varchar("region_code").bindTo { it.regionCode }
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }
}


@Repository
internal open class TestScopedOrderDao : BaseCrudDao<String, TestScopedOrder, TestScopedOrders>()


/**
 * The counter-example: same table shape, no declaration. Proves that opting in is what turns
 * filtering on, and that an undeclared entity is untouched even for a restricted subject.
 */
internal interface TestUnscopedOrder : IDbEntity<String, TestUnscopedOrder> {

    companion object Companion : DbEntityFactory<TestUnscopedOrder>()

    var title: String
    var orgId: String?
}


internal object TestUnscopedOrders : StringIdTable<TestUnscopedOrder>("test_scoped_order") {
    var title = varchar("title").bindTo { it.title }
    var orgId = varchar("org_id").bindTo { it.orgId }
}


@Repository
internal open class TestUnscopedOrderDao : BaseCrudDao<String, TestUnscopedOrder, TestUnscopedOrders>()
