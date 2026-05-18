package io.kudos.ms.sys.api.internal.controller.i18n

import io.kudos.ms.sys.common.i18n.api.ISysI18nApi
import io.kudos.ms.sys.common.i18n.vo.request.SysI18nFormUpdate
import io.kudos.ms.sys.core.i18n.api.SysI18nApi
import org.springframework.web.bind.annotation.RestController


/**
 * 国际化 内部 RPC 控制器。路径继承自 [ISysI18nApi] 方法级注解。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysI18nInternalController(
    private val sysI18nApi: SysI18nApi,
) : ISysI18nApi {

    override fun getI18nValue(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
        key: String,
    ): String? = sysI18nApi.getI18nValue(locale, i18nTypeDictCode, namespace, atomicServiceCode, key)

    override fun getI18ns(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
    ): Map<String, String> = sysI18nApi.getI18ns(locale, i18nTypeDictCode, namespace, atomicServiceCode)

    override fun batchSaveOrUpdate(i18ns: List<SysI18nFormUpdate>): Int =
        sysI18nApi.batchSaveOrUpdate(i18ns)

    override fun updateActive(id: String, active: Boolean): Boolean =
        sysI18nApi.updateActive(id, active)

}
