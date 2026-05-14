package io.kudos.ms.sys.client.dict.support

import io.kudos.base.bean.validation.terminal.convert.converter.IDictItemCodeFinder
import io.kudos.context.kit.SpringKit
import io.kudos.ms.sys.client.dict.proxy.ISysDictProxy

/**
 * 字典项编码查找器（Feign 实现）。
 *
 * 服务于**不部署 sys-core** 的下游微服务：当其 VO 上有 `@DictItemCode` 校验时，通过 Feign 远程拿活跃字典码。
 *
 * **与 sys-core 的 `DictItemCodeFinder`（基于 Hash 缓存）互斥：** 两者均通过 `ServiceLoader` 暴露 `IDictItemCodeFinder`，
 * 同一 deployment 通常只会加载到其中一个（sys 部署侧依赖 core，下游服务依赖 client），不会同时上线。
 *
 * 通过 `ServiceLoader` 加载，**不能**用构造注入，bean 仅在首次校验时按需 lazy 取。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
class FeignDictItemCodeFinder : IDictItemCodeFinder {

    @Volatile
    private var dictProxy: ISysDictProxy? = null

    override fun getDictItemCodes(atomicServiceCode: String, dictType: String): Set<String> {
        val proxy = dictProxy ?: SpringKit.getBean<ISysDictProxy>().also { dictProxy = it }
        return proxy.getActiveDictItems(dictType, atomicServiceCode)
            .map { it.itemCode }
            .toSet()
    }
}
