package io.kudos.ms.sys.core.i18n.service.iservice
import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.i18n.vo.SysI18nCacheEntry
import io.kudos.ms.sys.common.i18n.vo.request.SysI18nFormUpdate
import io.kudos.ms.sys.core.i18n.model.po.SysI18n


/**
 * 国际化业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysI18nService : IBaseCrudService<String, SysI18n> {

    /**
     * 按主键从 Hash 缓存加载单条国际化（未命中则回库并回写）
     */
    fun getI18nFromCache(id: String): SysI18nCacheEntry?

    /**
     * 在指定维度下获取某 key 的译文（来自 [getI18nsFromCache] 的 Map）
     */
    fun getI18nValueFromCache(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
        key: String
    ): String?

    /**
     * 按 locale、类型、命名空间、原子服务从 Hash 缓存加载 key→译文 Map（仅启用条目参与索引）
     */
    fun getI18nsFromCache(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
    ): Map<String, String>

    /**
     * 按多种类型与命名空间批量从缓存合并译文
     */
    fun batchGetI18nsFromCache(
        locale: String,
        namespacesByI18nTypeDictCode: Map<String, Collection<String>>,
        atomicServiceCodes: Collection<String>,
    ): Map<String, Map<String, Map<String, String>>>

    /**
     * 批量保存或更新国际化内容
     */
    fun batchSaveOrUpdate(i18ns: List<SysI18nFormUpdate>): Int

    /**
     * 更新启用状态，并同步 Hash 缓存
     */
    fun updateActive(id: String, active: Boolean): Boolean

}
