package io.kudos.ability.cache.common.support

import io.kudos.ability.cache.common.aop.TenantCacheEvict
import io.kudos.ability.cache.common.aop.TenantCachePut
import io.kudos.ability.cache.common.aop.TenantCacheable
import io.kudos.context.core.KudosContextHolder
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.ParameterNameDiscoverer
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import java.lang.reflect.Method
import java.util.*

/**
 * TenantCacheable的key生成规则，增加上下文
 */
class TenantCacheKeyGenerator : KeyGenerator {
    private val parameterNameDiscoverer: ParameterNameDiscoverer = DefaultParameterNameDiscoverer()

    private val parser: ExpressionParser = SpelExpressionParser()

    fun generalNormalKey(target: Any, method: Method, key: String, vararg params: Any?): Any {
        var key = key
        if (key.isNotBlank()) {
            key = "#tenantId.concat('::').concat($key)"
        } else {
            key = "#tenantId"
        }
        return generateKey(target, method, key, *params)
    }

    override fun generate(target: Any, method: Method, vararg params: Any?): Any {
        var key = getAnnotationKey(method)
        key = if (key.isNotBlank()) {
            "#tenantId.concat('::').concat($key)"
        } else {
            "#tenantId"
        }
        return generateKey(target, method, key, *params)
    }

    private fun getAnnotationKey(method: Method): String {
        return KEY_ANNOTATIONS.stream()
            .map { type: Class<out Annotation?>? -> AnnotationUtils.findAnnotation(method, type) }
            .filter { obj: Any? -> Objects.nonNull(obj) }
            .map<String> { ann: Annotation? -> AnnotationUtils.getValue(ann, "suffix") as String? }
            .filter { obj: String? -> Objects.nonNull(obj) }
            .findFirst()
            .orElse("")
    }

    private fun generateKey(target: Any, method: Method, cacheKey: String, vararg params: Any?): String {
        val context: StandardEvaluationContext =
            MethodBasedEvaluationContext(target, method, params, parameterNameDiscoverer)
        context.setRootObject(target)
        context.setVariable("method", method)
        context.setVariable("tenantId", KudosContextHolder.get().tenantId)
        val expression = parser.parseExpression(cacheKey)
        return expression.getValue(context, String::class.java)!!
    }

    companion object {
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
