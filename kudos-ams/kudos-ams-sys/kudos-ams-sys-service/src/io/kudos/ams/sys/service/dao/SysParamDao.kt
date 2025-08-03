package io.kudos.ams.sys.service.dao

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.ability.data.rdb.ktorm.kit.getDatabase
import io.kudos.ams.sys.service.model.po.SysParam
import io.kudos.ams.sys.service.model.table.SysParams
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ams.sys.common.vo.param.SysParamCacheItem
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
     * @param moduleCode 模块编码
     * @param paramName 参数名称
     * @return SysParamCacheItem，找不到返回null
     */
    open fun getActiveParamsForCache(moduleCode: String, paramName: String): SysParamCacheItem? {
        return querySource()
            .select(SysParams.columns)
            .whereWithConditions {
                it += (SysParams.paramName eq paramName) and (SysParams.active eq true)
                if (moduleCode.isNotEmpty()) {
                    it += SysParams.moduleCode eq moduleCode
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