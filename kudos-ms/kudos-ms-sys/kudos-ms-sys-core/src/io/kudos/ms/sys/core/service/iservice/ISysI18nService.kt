package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ms.sys.common.vo.i18n.request.SysI18nFormUpdate
import io.kudos.ms.sys.core.model.po.SysI18n


/**
 * 国际化业务接口
 *
 * @author K
 * @since 1.0.0
 */
interface ISysI18nService : IBaseCrudService<String, SysI18n> {


    /**
     * 获取国际化值
     *
     * @param locale 语言地区
     * @param i18nTypeDictCode 国际化类型字典代码
     * @param namespace 命名空间
     * @param atomicServiceCode 原子服务编码
     * @param key 国际化key
     * @return 国际化值，找不到返回null
     */
    fun getI18nValue(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
        key: String
    ): String?

    /**
     * 获取指定参数的国际化信息
     *
     * @param locale 语言地区
     * @param i18nTypeDictCode 国际化类型字典代码
     * @param namespace 命名空间
     * @param atomicServiceCode 原子服务编码
     * @return Map<国际化key, 译文>
     */
    fun getI18ns(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
    ): Map<String, String>

    /**
     * 批量获取国际化信息
     *
     * @param locale 语言地区
     * @param namespacesByI18nTypeDictCode Map<国际化类型字典代码，Collection<命名空间>>
     * @param atomicServiceCodes 原子服务编码集合
     * @return Map<国际化类型字典代码，Map<命名空间，Map<国际化key, 译文>>>
     */
    fun batchGetI18ns(
        locale: String,
        namespacesByI18nTypeDictCode: Map<String, Collection<String>>,
        atomicServiceCodes: Collection<String>,
    ): Map<String, Map<String, Map<String, String>>>


    /**
     * 批量保存或更新国际化内容
     *
     * @param i18ns 国际化载体列表
     * @return 成功保存或更新的数量
     * @author K
     * @since 1.0.0
     */
    fun batchSaveOrUpdate(i18ns: List<SysI18nFormUpdate>): Int

    /**
     * 更新启用状态
     *
     * @param id 国际化id
     * @param active 是否启用
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean


}
