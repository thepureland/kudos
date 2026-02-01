package io.kudos.ams.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.sys.core.model.po.SysI18n
import io.kudos.ams.sys.common.vo.i18n.SysI18nRecord
import io.kudos.ams.sys.common.vo.i18n.SysI18nPayload


/**
 * 国际化业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysI18nService : IBaseCrudService<String, SysI18n> {
//endregion your codes 1

    //region your codes 2

    /**
     * 获取国际化值
     *
     * @param locale 语言地区
     * @param atomicServiceCode 原子服务编码
     * @param i18nTypeDictCode 国际化类型字典代码
     * @param key 国际化key
     * @return 国际化值，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getI18nValue(locale: String, atomicServiceCode: String, i18nTypeDictCode: String, key: String): String?

    /**
     * 获取国际化列表
     *
     * @param atomicServiceCode 原子服务编码
     * @param i18nTypeDictCode 国际化类型字典代码
     * @param locale 语言地区，为null时返回所有语言
     * @return 国际化记录列表
     * @author K
     * @since 1.0.0
     */
    fun getI18nsByAtomicServiceAndType(atomicServiceCode: String, i18nTypeDictCode: String, locale: String? = null): List<SysI18nRecord>

    /**
     * 批量保存或更新国际化内容
     *
     * @param i18ns 国际化载体列表
     * @return 成功保存或更新的数量
     * @author K
     * @since 1.0.0
     */
    fun batchSaveOrUpdate(i18ns: List<SysI18nPayload>): Int

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

    //endregion your codes 2

}
