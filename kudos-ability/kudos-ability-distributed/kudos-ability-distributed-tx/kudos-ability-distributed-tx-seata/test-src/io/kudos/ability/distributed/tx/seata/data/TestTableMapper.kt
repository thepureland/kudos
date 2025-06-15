package io.kudos.ability.distributed.tx.seata.data

import org.apache.ibatis.annotations.Mapper
import org.soul.ability.data.rdb.mybatis.imapper.IBaseCrudMapper
import org.springframework.stereotype.Repository

/**
 * 测试表数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Mapper
@Repository
interface TestTableMapper : IBaseCrudMapper<TestTable, Int>
