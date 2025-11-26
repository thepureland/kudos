package io.kudos.ability.cache.common.support

import io.kudos.context.core.KudosContextHolder
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.ExpressionParser
import org.springframework.expression.TypedValue
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import java.lang.reflect.Method

/**
 * Cacheable的key生成规则，增加上下文
 */
class ContextKeyGenerator : KeyGenerator {

    private val parameterNameDiscoverer = DefaultParameterNameDiscoverer()

    override fun generate(target: Any, method: Method, vararg params: Any?): Any {
        val annotation = method.getAnnotation<CacheKey>(CacheKey::class.java)
        requireNotNull(annotation) { "使用ContextKeyGenerator，需额外增加@CacheKey配置" }
        val key: String? = annotation.value
        require(!key.isNullOrBlank()) { "@CacheKey未指定规则" }

        return generateKey(method, key, *params)
    }

    private fun generateKey(method: Method, cacheKey: String, vararg params: Any?): String {
        val context: StandardEvaluationContext =
            MethodBasedEvaluationContext(TypedValue.NULL, method, params, parameterNameDiscoverer)
        val kudosContext = KudosContextHolder.get()
//        kudosContext.subSysCode = ApplicationInfo.getInstance().getSubSysCode()
        context.setVariable("_context", kudosContext)
        val parser: ExpressionParser = SpelExpressionParser()
        val expression = parser.parseExpression(cacheKey)
        return expression.getValue(context, String::class.java)!!
    }
}
