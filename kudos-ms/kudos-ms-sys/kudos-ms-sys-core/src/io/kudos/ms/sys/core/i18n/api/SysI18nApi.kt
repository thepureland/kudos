package io.kudos.ms.sys.core.i18n.api

import io.kudos.ms.sys.common.i18n.api.ISysI18nApi
import io.kudos.ms.sys.common.i18n.vo.request.SysI18nFormUpdate
import io.kudos.ms.sys.core.i18n.service.iservice.ISysI18nService
import org.springframework.stereotype.Service


/**
 * 国际化 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class SysI18nApi(
    private val sysI18nService: ISysI18nService,
) : ISysI18nApi {

    override fun getI18nValue(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
        key: String
    ): String? = sysI18nService.getI18nValueFromCache(locale, i18nTypeDictCode, namespace, atomicServiceCode, key)

    override fun getI18ns(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String
    ): Map<String, String> = sysI18nService.getI18nsFromCache(locale, i18nTypeDictCode, namespace, atomicServiceCode)

    override fun batchSaveOrUpdate(i18ns: List<SysI18nFormUpdate>): Int = sysI18nService.batchSaveOrUpdate(i18ns)

    override fun updateActive(id: String, active: Boolean): Boolean = sysI18nService.updateActive(id, active)
}
