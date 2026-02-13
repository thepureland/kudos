package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.vo.i18n.SysI18nCacheItem
import io.kudos.ms.sys.core.model.po.SysI18n
import io.kudos.ms.sys.core.model.table.SysI18ns
import org.springframework.stereotype.Repository


/**
 * 国际化数据访问对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysI18nDao : BaseCrudDao<String, SysI18n, SysI18ns>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据语言、类型、原子服务编码获取对应的启用的国际化内容（for cache）
     *
     * @param locale 语言_地区
     * @param i18nTypeDictCode 国际化类型字典代码
     * @param atomicServiceCode 原子服务编码
     * @return List<SysI18nCacheItem>，找不到返回空列表
     */
    open fun fetchActiveI18nsForCache(
        locale: String,
        i18nTypeDictCode: String,
        atomicServiceCode: String
    ): List<SysI18nCacheItem> {
        val criteria = Criteria.and(
            SysI18n::locale eq locale,
            SysI18n::i18nTypeDictCode eq i18nTypeDictCode,
            SysI18n::atomicServiceCode eq atomicServiceCode,
            SysI18n::active eq true
        )
        return searchAs<SysI18nCacheItem>(criteria)
    }

    /**
     * 获取所有启用的国际化内容（for cache）
     *
     * @return List<SysI18nCacheItem>
     */
    open fun fetchAllActiveI18nsForCache(): List<SysI18nCacheItem> {
        val criteria = Criteria(SysI18n::active eq true)
        return searchAs<SysI18nCacheItem>(criteria)
    }

    //endregion your codes 2

}
