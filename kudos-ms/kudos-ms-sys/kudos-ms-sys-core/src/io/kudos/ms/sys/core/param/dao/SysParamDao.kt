package io.kudos.ms.sys.core.param.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.sys.common.param.vo.SysParamCacheEntry
import io.kudos.ms.sys.core.param.model.po.SysParam
import io.kudos.ms.sys.core.param.model.table.SysParams
import org.ktorm.dsl.*
import org.springframework.stereotype.Repository


/**
 * 参数数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysParamDao : BaseCrudDao<String, SysParam, SysParams>() {


    /**
     * 根据模块和参数名获取对应的启用的参数（for cache）
     *
     * @param atomicServiceCode 原子服务编码
     * @param paramName 参数名称
     * @return SysParamCacheEntry，找不到返回null
     */
    open fun getActiveParamsForCache(atomicServiceCode: String, paramName: String): SysParamCacheEntry? {
        return querySource()
            .select(SysParams.columns)
            .whereWithConditions {
                it += (SysParams.paramName eq paramName) and (SysParams.active eq true)
                if (atomicServiceCode.isNotEmpty()) {
                    it += SysParams.atomicServiceCode eq atomicServiceCode
                }
            }
            .map { row ->
                SysParamCacheEntry(
                    id = row[SysParams.id].orEmpty(),
                    paramName = row[SysParams.paramName].orEmpty(),
                    paramValue = row[SysParams.paramValue].orEmpty(),
                    defaultValue = row[SysParams.defaultValue],
                    atomicServiceCode = row[SysParams.atomicServiceCode].orEmpty(),
                    orderNum = row[SysParams.orderNum],
                    remark = row[SysParams.remark],
                    active = row[SysParams.active] ?: true,
                    builtIn = row[SysParams.builtIn] ?: true
                )
            }
            .toList().firstOrNull()
    }


}