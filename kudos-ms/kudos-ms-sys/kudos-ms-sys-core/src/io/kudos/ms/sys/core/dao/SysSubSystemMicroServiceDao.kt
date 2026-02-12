package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
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
        val criteria = Criteria(SysSubSystemMicroService::subSystemCode eq subSystemCode)
        return searchProperty(criteria, SysSubSystemMicroService::microServiceCode).toSet()
    }

    /**
     * 根据微服务编码查找对应的子系统编码
     *
     * @param microServiceCode 微服务编码
     * @return Set<子系统编码>
     */
    fun fetchSubSystemCodesByMicroServiceCode(microServiceCode: String): Set<String> {
        val criteria = Criteria(SysSubSystemMicroService::microServiceCode eq microServiceCode)
        return searchProperty(criteria, SysSubSystemMicroService::subSystemCode).toSet()
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
            SysSubSystemMicroService::subSystemCode eq subSystemCode,
            SysSubSystemMicroService::microServiceCode eq microServiceCode
        )
        return count(criteria) > 0
    }

    /**
     * 按子系统编码和微服务编码删除关系
     *
     * @param subSystemCode 子系统编码
     * @param microServiceCode 微服务编码
     * @return 删除条数
     */
    fun deleteBySubSystemCodeAndMicroServiceCode(subSystemCode: String, microServiceCode: String): Int {
        val criteria = Criteria.and(
            SysSubSystemMicroService::subSystemCode eq subSystemCode,
            SysSubSystemMicroService::microServiceCode eq microServiceCode
        )
        return batchDeleteCriteria(criteria)
    }

    //endregion your codes 2

}