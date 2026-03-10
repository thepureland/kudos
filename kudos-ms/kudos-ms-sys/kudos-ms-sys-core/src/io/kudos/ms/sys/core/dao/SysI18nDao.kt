package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.vo.i18n.SysI18nCacheEntry
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
     * 根据语言、类型、命名空间、原子服务编码获取对应的启用的国际化内容（for cache）。
     * namespace 传空时不过滤 namespace，仅按 locale、atomicServiceCode、i18nTypeDictCode 查询。
     *
     * @param locale 语言-地区
     * @param atomicServiceCode 原子服务编码
     * @param i18nTypeDictCode 国际化类型字典代码
     * @param namespace 命名空间，缺省为null，为null不参与查询
     * @return List<SysI18nCacheEntry>，找不到返回空列表
     */
    open fun fetchActiveI18nsForCache(
        locale: String,
        atomicServiceCode: String,
        i18nTypeDictCode: String,
        namespace: String? = null
    ): List<SysI18nCacheEntry> {
        val criteria = Criteria.and(
            SysI18n::locale eq locale,
            SysI18n::i18nTypeDictCode eq i18nTypeDictCode,
            SysI18n::atomicServiceCode eq atomicServiceCode,
            SysI18n::active eq true
        )
        if (!namespace.isNullOrBlank()) {
            criteria.addAnd(SysI18n::namespace eq namespace)
        }
        return searchAs<SysI18nCacheEntry>(criteria)
    }

    /**
     * 获取所有启用的国际化内容（for cache）
     *
     * @return List<SysI18nCacheEntry>
     */
    open fun fetchAllActiveI18nsForCache(): List<SysI18nCacheEntry> {
        val criteria = Criteria(SysI18n::active eq true)
        return searchAs<SysI18nCacheEntry>(criteria)
    }

    //endregion your codes 2

}
