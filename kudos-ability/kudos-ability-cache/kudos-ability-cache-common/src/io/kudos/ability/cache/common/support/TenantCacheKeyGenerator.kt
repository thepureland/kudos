package io.kudos.ability.cache.common.support

import io.kudos.ability.cache.common.aop.keyvalue.TenantCacheEvict
import io.kudos.ability.cache.common.aop.keyvalue.TenantCachePut
import io.kudos.ability.cache.common.aop.keyvalue.TenantCacheable
import io.kudos.context.core.KudosContextHolder
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.ParameterNameDiscoverer
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.expression.spel.support.StandardEvaluationContext
import java.lang.reflect.Method

/**
 * Tenant-aware cache key generator.
 *
 * Generates cache keys that include the tenant ID for annotations such as TenantCacheable, providing multi-tenant isolation.
 *
 * Core responsibilities:
 * 1. Auto tenant ID prefix: prefixes every generated key with the tenant ID.
 * 2. SpEL expression support: dynamically generates keys via SpEL expressions.
 * 3. Annotation extraction: extracts the key configuration (suffix attribute) from method annotations.
 * 4. Context variables: exposes variables such as method and tenantId for use in SpEL.
 *
 * Key formats:
 * - When suffix is specified: "{tenantId}::{result of the suffix expression}".
 * - When suffix is not specified: "{tenantId}".
 *
 * Supported annotations:
 * - TenantCacheable, TenantCacheEvict, TenantCachePut.
 * - CachePut, Cacheable, CacheEvict (standard annotations for compatibility).
 *
 * SpEL expressions:
 * - Reference method parameters (#argName or #p0).
 * - Reference the method object (#method).
 * - Reference the tenant ID (#tenantId).
 * - Reference the target object (#root.target).
 *
 * Notes:
 * - Tenant ID is taken from KudosContext.
 * - If suffix is empty, only the tenant ID is returned.
 * - SpEL parsing supports complex key generation logic.
 */
class TenantCacheKeyGenerator : KeyGenerator {
    /** SpEL parameter name discoverer so expressions can reference method formal parameters via `#paramName`. */
    private val parameterNameDiscoverer: ParameterNameDiscoverer = SpelExpressionCache.parameterNameDiscoverer

    /**
     * Generates a general key (with tenant ID).
     *
     * Builds a cache key that includes the tenant ID based on the provided key string.
     *
     * Workflow:
     * 1. Check the key: if non-empty, concatenate the tenant ID and the key.
     * 2. If empty: return only the tenant ID.
     * 3. Invoke generateKey: resolve via SpEL and produce the final key.
     *
     * Concatenation rules:
     * - If key is non-empty: use the expression "#tenantId.concat('::').concat($key)".
     * - If key is empty: use the expression "#tenantId".
     *
     * @param target target object
     * @param method target method
     * @param key raw key string (SpEL expression)
     * @param params method parameters
     * @return generated cache key (including tenant ID)
     */
    fun generalNormalKey(target: Any, method: Method, key: String, vararg params: Any?): Any {
        val keyStr = if (key.isNotBlank()) {
            "#tenantId.concat('::').concat($key)"
        } else {
            "#tenantId"
        }
        return generateKey(target, method, keyStr, *params)
    }

    /**
     * Generates a cache key (implements the KeyGenerator interface).
     *
     * Extracts the key configuration from method annotations and produces a cache key containing the tenant ID.
     *
     * Workflow:
     * 1. Extract the annotation key: read the suffix attribute from the method's annotations.
     * 2. Concatenate the tenant ID: prepend the tenant ID to the suffix when non-empty.
     * 3. Invoke generateKey: resolve via SpEL and produce the final key.
     *
     * Annotation lookup order:
     * - TenantCacheable, TenantCacheEvict, TenantCachePut.
     * - CachePut, Cacheable, CacheEvict.
     * - The first annotation with a suffix wins.
     *
     * @param target target object
     * @param method target method
     * @param params method parameters
     * @return generated cache key (including tenant ID)
     */
    override fun generate(target: Any, method: Method, vararg params: Any?): Any {
        var key = getAnnotationKey(method)
        key = if (key.isNotBlank()) {
            "#tenantId.concat('::').concat($key)"
        } else {
            "#tenantId"
        }
        return generateKey(target, method, key, *params)
    }

    /**
     * Extracts the key configuration from method annotations.
     *
     * Iterates the supported annotation types and returns the suffix attribute of the first one that defines it.
     *
     * Lookup order:
     * 1. TenantCacheable.
     * 2. TenantCacheEvict.
     * 3. TenantCachePut.
     * 4. CachePut.
     * 5. Cacheable.
     * 6. CacheEvict.
     *
     * Return value:
     * - The first found suffix, or an empty string if none of the annotations provide one or are present.
     *
     * @param method target method
     * @return the suffix extracted from the annotations, or an empty string if absent
     */
    private fun getAnnotationKey(method: Method): String =
        KEY_ANNOTATIONS.firstNotNullOfOrNull { type ->
            AnnotationUtils.findAnnotation(method, type)
                ?.let { AnnotationUtils.getValue(it, "suffix") as? String }
        } ?: ""

    /**
     * Generates a cache key using a SpEL expression.
     *
     * Builds the SpEL evaluation context, parses the cacheKey expression, and produces the final cache key.
     *
     * Workflow:
     * 1. Build the evaluation context: use MethodBasedEvaluationContext with method parameter info.
     * 2. Set the root object: use target as the root.
     * 3. Set variables:
     *    - method: target method object.
     *    - tenantId: tenant ID fetched from KudosContext.
     * 4. Parse the expression: use the SpEL parser on cacheKey.
     * 5. Read the value: get the value as a String from the expression.
     *
     * SpEL context variables:
     * - #method: target method object.
     * - #tenantId: current tenant ID.
     * - #root.target: target object.
     * - #argName or #p0: method parameters (parameter names resolved via parameterNameDiscoverer).
     *
     * Expression examples:
     * - "#tenantId": returns the tenant ID only.
     * - "#tenantId.concat('::').concat(#id)": returns "<tenantId>::<id>".
     * - "#tenantId + '::' + #user.id": returns "<tenantId>::<userId>".
     *
     * Notes:
     * - cacheKey must be a valid SpEL expression.
     * - The expression must return a String.
     * - Parsing failures throw an exception.
     *
     * @param target target object
     * @param method target method
     * @param cacheKey SpEL expression string
     * @param params method parameters
     * @return the resolved cache key string
     */
    private fun generateKey(target: Any, method: Method, cacheKey: String, vararg params: Any?): String {
        val context: StandardEvaluationContext =
            MethodBasedEvaluationContext(target, method, params, parameterNameDiscoverer)
        context.setRootObject(target)
        context.setVariable("method", method)
        context.setVariable("tenantId", KudosContextHolder.get().tenantId)
        return requireNotNull(SpelExpressionCache.get(cacheKey).getValue(context, String::class.java)) {
            "SpEL cache key must resolve to non-null String"
        }
    }

    companion object {
        /**
         * Annotation lookup order: custom Tenant* annotations first, then Spring's standard [CachePut]/[Cacheable]/[CacheEvict].
         * Custom ones take priority because business code typically uses the higher-semantics annotations; the standard ones serve as a compatibility fallback.
         */
        private val KEY_ANNOTATIONS = listOf(
            TenantCacheable::class.java,
            TenantCacheEvict::class.java,
            TenantCachePut::class.java,
            CachePut::class.java,
            Cacheable::class.java,
            CacheEvict::class.java
        )
    }
}
