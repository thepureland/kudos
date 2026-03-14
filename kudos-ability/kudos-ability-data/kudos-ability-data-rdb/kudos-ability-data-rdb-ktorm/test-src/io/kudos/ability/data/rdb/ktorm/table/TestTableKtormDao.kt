package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import org.springframework.stereotype.Repository

/**
 * 测试表数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
internal open class TestTableKtormDao: BaseCrudDao<Int, TestTableKtorm, TestTableKtorms>() {



}