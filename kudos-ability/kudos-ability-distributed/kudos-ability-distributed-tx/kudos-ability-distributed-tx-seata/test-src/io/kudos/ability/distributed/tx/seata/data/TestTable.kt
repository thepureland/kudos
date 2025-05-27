package io.kudos.ability.distributed.tx.seata.data

import org.soul.base.bean.IEntity

/**
 * 测试表数据库实体
 *
 * @author will
 * @since 5.1.1
 */
class TestTable : IEntity<Int?> {
    private var id: Int? = null

    var balance: Double? = null

    override fun getId(): Int? {
        return id
    }

    override fun setId(id: Int?) {
        this.id = id
    }
}
