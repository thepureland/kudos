package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.core.model.po.SysSubSystemMicroService
import io.kudos.ms.sys.core.model.table.SysSubSystemMicroServices
import org.springframework.stereotype.Repository


/**
 * 子系统-微服务关系数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysSubSystemMicroServiceDao : BaseCrudDao<String, SysSubSystemMicroService, SysSubSystemMicroServices>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据子系统编码查找对应的微服务编码
     *
     * @param subSystemCode 子系统编码
     * @return Set<微服务编码>
     */
    fun searchMicroServiceCodesBySubSystemCode(subSystemCode: String): Set<String> {
        val criteria = Criteria.of(SysSubSystemMicroService::subSystemCode.name, OperatorEnum.EQ, subSystemCode)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, SysSubSystemMicroService::microServiceCode.name).toSet() as Set<String>
    }

    /**
     * 根据微服务编码查找对应的子系统编码
     *
     * @param microServiceCode 微服务编码
     * @return Set<子系统编码>
     */
    fun fetchSubSystemCodesByMicroServiceCode(microServiceCode: String): Set<String> {
        val criteria = Criteria(SysSubSystemMicroService::microServiceCode.name, OperatorEnum.EQ, microServiceCode)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, SysSubSystemMicroService::subSystemCode.name).toSet() as Set<String>
    }

    /**
     * 检查关系是否存在
     *
     * @param subSystemCode 子系统编码
     * @param microServiceCode 微服务编码
     * @return 是否存在
     */
    fun exists(subSystemCode: String, microServiceCode: String): Boolean {
        val criteria = Criteria.and(
            Criterion(SysSubSystemMicroService::subSystemCode.name, OperatorEnum.EQ, subSystemCode),
            Criterion(SysSubSystemMicroService::microServiceCode.name, OperatorEnum.EQ, microServiceCode)
        )
        return count(criteria) > 0
    }

    //endregion your codes 2

}