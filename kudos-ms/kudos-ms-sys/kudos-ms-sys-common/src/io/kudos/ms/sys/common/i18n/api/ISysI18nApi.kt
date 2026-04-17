package io.kudos.ms.sys.common.i18n.api

import io.kudos.ms.sys.common.i18n.vo.request.SysI18nFormUpdate


/**
 * 国际化 对外API
 *
 * @author K
 * @since 1.0.0
 */
interface ISysI18nApi {


    fun getI18nValue(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
        key: String
    ): String?

    fun getI18ns(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String
    ): Map<String, String>

    fun batchSaveOrUpdate(i18ns: List<SysI18nFormUpdate>): Int

    fun updateActive(id: String, active: Boolean): Boolean


}
