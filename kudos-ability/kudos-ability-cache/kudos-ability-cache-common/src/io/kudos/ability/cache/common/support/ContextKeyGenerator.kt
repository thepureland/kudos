package io.kudos.ability.cache.common.support

import org.soul.ability.cache.common.support.CacheKey
import org.soul.base.lang.string.StringTool
import org.soul.context.core.ApplicationInfo
import org.soul.context.core.CommonContext
import org.soul.context.core.ContextParam
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.ParameterNameDiscoverer
import org.springframework.expression.ExpressionParser
import org.springframework.expression.TypedValue
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import java.lang.reflect.Method

/**
 * Cacheable的key生成规则，增加上下文
 */
class ContextKeyGenerator : KeyGenerator {
    private val parameterNameDiscoverer: ParameterNameDiscoverer = DefaultParameterNameDiscoverer()

    override fun generate(target: Any, method: Method, vararg params: Any?): Any {
        val annotation = method.getAnnotation<CacheKey>(CacheKey::class.java)
        requireNotNull(annotation) { "使用ContextKeyGenerator，需额外增加@CacheKey配置" }
        val key: String? = annotation.value
        require(!StringTool.isBlank(key)) { "@CacheKey未指定规则" }

        return generateKey(method, key!!, *params)
    }

    private fun generateKey(method: Method, cacheKey: String, vararg params: Any?): String {
        val context: StandardEvaluationContext =
            MethodBasedEvaluationContext(TypedValue.NULL, method, params, parameterNameDiscoverer)
        var contextParam = CommonContext.get()
        if (contextParam == null) {
            contextParam = ContextParam()
            contextParam.setSubSysCode(ApplicationInfo.getInstance().getSubSysCode())
        }
        context.setVariable("_context", contextParam)
        val parser: ExpressionParser = SpelExpressionParser()
        val expression = parser.parseExpression(cacheKey)
        return expression.getValue<String>(context, String::class.java)
    }
}
