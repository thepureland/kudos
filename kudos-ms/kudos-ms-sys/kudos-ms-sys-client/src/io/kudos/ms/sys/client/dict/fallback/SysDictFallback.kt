package io.kudos.ms.sys.client.dict.fallback

import io.kudos.ms.sys.client.dict.proxy.ISysDictProxy
import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import org.springframework.stereotype.Component


/**
 * Dictionary Feign fallback implementation: logs and returns empty collections when the remote is unavailable,
 * letting callers (typically used for validation or dropdown data) continue with "no available dictionary" instead of aborting with an exception.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysDictFallback : SysClientFallbackSupport("SysDictFallback"), ISysDictProxy {

    override fun getActiveDictItems(
        dictType: String,
        atomicServiceCode: String,
    ): List<SysDictItemCacheEntry> {
        warnRead("getActiveDictItems", dictType, atomicServiceCode)
        return emptyList()
    }

    override fun getActiveDictItemMap(
        dictType: String,
        atomicServiceCode: String,
    ): LinkedHashMap<String, String> {
        warnRead("getActiveDictItemMap", dictType, atomicServiceCode)
        return LinkedHashMap()
    }

    override fun batchGetActiveDictItems(
        dictTypeAndASCodePairs: List<Pair<String, String>>,
    ): Map<Pair<String, String>, List<SysDictItemCacheEntry>> {
        warnRead("batchGetActiveDictItems", dictTypeAndASCodePairs)
        return emptyMap()
    }

    override fun batchGetActiveDictItemMap(
        dictTypeAndASCodePairs: List<Pair<String, String>>,
    ): Map<Pair<String, String>, LinkedHashMap<String, String>> {
        warnRead("batchGetActiveDictItemMap", dictTypeAndASCodePairs)
        return emptyMap()
    }
}
