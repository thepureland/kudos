package io.kudos.ams.sys.core.dao

import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ams.sys.core.model.po.SysMicroServiceAtomicService
import io.kudos.ams.sys.core.model.table.SysMicroServiceAtomicServices
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 微服务-原子服务关系数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysMicroServiceAtomicServiceDao : BaseCrudDao<String, SysMicroServiceAtomicService, SysMicroServiceAtomicServices>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据微服务编码查找对应的原子服务编码
     *
     * @param microServiceCode 微服务编码
     * @return Set<原子服务编码>
     */
    fun searchAtomicServiceCodesByMicroServiceCode(microServiceCode: String): Set<String> {
        val criteria = Criteria.of(SysMicroServiceAtomicService::microServiceCode.name, OperatorEnum.EQ, microServiceCode)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, SysMicroServiceAtomicService::atomicServiceCode.name).toSet() as Set<String>
    }

    /**
     * 根据原子服务编码查找对应的微服务编码
     *
     * @param atomicServiceCode 原子服务编码
     * @return Set<微服务编码>
     */
    fun searchMicroServiceCodesByAtomicServiceCode(atomicServiceCode: String): Set<String> {
        val criteria = Criteria.of(SysMicroServiceAtomicService::atomicServiceCode.name, OperatorEnum.EQ, atomicServiceCode)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, SysMicroServiceAtomicService::microServiceCode.name).toSet() as Set<String>
    }

    /**
     * 检查关系是否存在
     *
     * @param microServiceCode 微服务编码
     * @param atomicServiceCode 原子服务编码
     * @return 是否存在
     * @author AI: Cursor
     */
    fun exists(microServiceCode: String, atomicServiceCode: String): Boolean {
        val criteria = Criteria.of(SysMicroServiceAtomicService::microServiceCode.name, OperatorEnum.EQ, microServiceCode)
            .addAnd(SysMicroServiceAtomicService::atomicServiceCode.name, OperatorEnum.EQ, atomicServiceCode)
        return count(criteria) > 0
    }

    //endregion your codes 2

}