package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.bean.BeanKit
import io.kudos.ms.sys.common.vo.param.SysParamCacheItem
import io.kudos.ms.sys.core.model.po.SysParam
import io.kudos.ms.sys.core.model.table.SysParams
import org.ktorm.dsl.*
import org.springframework.stereotype.Repository


/**
 * 参数数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysParamDao : BaseCrudDao<String, SysParam, SysParams>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据模块和参数名获取对应的启用的参数（for cache）
     *
     * @param atomicServiceCode 原子服务编码
     * @param paramName 参数名称
     * @return SysParamCacheItem，找不到返回null
     */
    open fun getActiveParamsForCache(atomicServiceCode: String, paramName: String): SysParamCacheItem? {
        return querySource()
            .select(SysParams.columns)
            .whereWithConditions {
                it += (SysParams.paramName eq paramName) and (SysParams.active eq true)
                if (atomicServiceCode.isNotEmpty()) {
                    it += SysParams.atomicServiceCode eq atomicServiceCode
                }
            }
            .map { row ->
                val entity = SysParams.createEntity(row)
                BeanKit.copyProperties(entity, SysParamCacheItem())
            }
            .toList().firstOrNull()
    }

    //endregion your codes 2

}