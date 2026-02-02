package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.ability.data.rdb.ktorm.kit.getDatabase
import io.kudos.ms.sys.core.model.po.SysParam
import io.kudos.ms.sys.core.model.table.SysParams
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.sys.common.vo.param.SysParamCacheItem
import io.kudos.base.bean.BeanKit
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.dsl.whereWithConditions


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