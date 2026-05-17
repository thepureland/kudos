package io.kudos.ms.sys.client.support

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport

/**
 * sys 模块 Feign Fallback 的命名空间别名，沿用上游
 * [AbstractFeignFallbackSupport][AbstractFeignFallbackSupport] 的全部能力。
 *
 * 保留这一层是为了不修改已落地的 17 个 sys-client Fallback 类的父类引用；
 * 新模块的 Fallback 建议直接继承 [AbstractFeignFallbackSupport]。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
abstract class SysClientFallbackSupport(componentName: String) :
    AbstractFeignFallbackSupport(componentName)
