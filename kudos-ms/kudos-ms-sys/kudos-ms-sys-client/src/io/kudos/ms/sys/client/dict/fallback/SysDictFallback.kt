package io.kudos.ms.sys.client.dict.fallback

import io.kudos.ms.sys.client.dict.proxy.ISysDictProxy
import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import org.springframework.stereotype.Component


/**
 * 字典 Feign 容错降级实现：远程不可用时记日志并返回空集合，
 * 让调用方（通常用于校验、下拉数据）以「无可用字典」继续运行而不是抛异常中断。
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
