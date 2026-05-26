package io.kudos.ms.sys.client.i18n.fallback

import io.kudos.ms.sys.client.i18n.proxy.ISysI18nProxy
import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import io.kudos.ms.sys.common.i18n.vo.request.SysI18nFormUpdate
import org.springframework.stereotype.Component


/**
 * I18n Feign client fallback implementation.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysI18nFallback : SysClientFallbackSupport("SysI18nFallback"), ISysI18nProxy {

    override fun getI18nValue(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
        key: String,
    ): String? {
        warnRead("getI18nValue", locale, i18nTypeDictCode, namespace, atomicServiceCode, key)
        return null
    }

    override fun getI18ns(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
    ): Map<String, String> {
        warnRead("getI18ns", locale, i18nTypeDictCode, namespace, atomicServiceCode)
        return emptyMap()
    }

    override fun batchSaveOrUpdate(i18ns: List<SysI18nFormUpdate>): Int {
        errorWrite("batchSaveOrUpdate", "size=${i18ns.size}")
        return 0
    }

    override fun updateActive(id: String, active: Boolean): Boolean {
        errorWrite("updateActive", id, active)
        return false
    }
}
