package io.kudos.ms.sys.client.support

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport

/**
 * Namespace alias for the sys module Feign Fallback, inheriting all capabilities
 * of upstream [AbstractFeignFallbackSupport][AbstractFeignFallbackSupport].
 *
 * This layer is retained to avoid modifying the parent references of the 17 sys-client
 * Fallback classes already in place; new modules' Fallbacks are recommended to extend
 * [AbstractFeignFallbackSupport] directly.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
abstract class SysClientFallbackSupport(componentName: String) :
    AbstractFeignFallbackSupport(componentName)
