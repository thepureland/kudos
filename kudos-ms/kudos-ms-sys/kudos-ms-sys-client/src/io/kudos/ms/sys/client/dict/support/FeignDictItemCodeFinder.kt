package io.kudos.ms.sys.client.dict.support

import io.kudos.base.bean.validation.terminal.convert.converter.IDictItemCodeFinder
import io.kudos.context.kit.SpringKit
import io.kudos.ms.sys.client.dict.proxy.ISysDictProxy

/**
 * Dictionary item code finder (Feign implementation).
 *
 * Serves downstream microservices that **do not deploy sys-core**: when their VOs carry `@DictItemCode` validation, active dictionary codes are fetched remotely via Feign.
 *
 * **Mutually exclusive with sys-core's `DictItemCodeFinder` (based on the hash cache):** both expose `IDictItemCodeFinder` via `ServiceLoader`,
 * and a given deployment typically only loads one (the sys deployment side depends on core, while downstream services depend on the client); they are never online at the same time.
 *
 * Loaded via `ServiceLoader`; constructor injection **cannot** be used, so the bean is lazily fetched on first validation.
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
