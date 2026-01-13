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
 * 上下文缓存key生成器
 * 
 * 为@CacheKey注解生成缓存key，支持在SpEL表达式中使用KudosContext上下文信息。
 * 
 * 核心功能：
 * 1. 注解驱动：从@CacheKey注解中读取SpEL表达式
 * 2. 上下文注入：将KudosContext注入到SpEL表达式中，变量名为"_context"
 * 3. 参数支持：支持在表达式中引用方法参数
 * 
 * 使用要求：
 * - 方法必须标注@CacheKey注解
 * - @CacheKey的value属性必须提供SpEL表达式
 * - 表达式必须返回String类型
 * 
 * SpEL表达式支持：
 * - 方法参数：使用#argName或#p0引用参数
 * - 上下文信息：使用#_context访问KudosContext对象
 *   - #_context.tenantId：租户ID
 *   - #_context.subSystemCode：子系统代码
 *   - #_context.traceKey：追踪键
 *   - 其他KudosContext的属性
 * 
 * 使用场景：
 * - 需要根据上下文信息生成缓存key
 * - 需要结合方法参数和上下文信息
 * - 需要动态生成复杂的缓存key
 * 
 * 注意事项：
 * - 必须使用@CacheKey注解，否则抛出异常
 * - 表达式不能为空，否则抛出异常
 * - 表达式解析失败会抛出异常
 */
class ContextKeyGenerator : KeyGenerator {

    private val parameterNameDiscoverer = DefaultParameterNameDiscoverer()

    /**
     * 生成缓存key（实现KeyGenerator接口）
     * 
     * 从@CacheKey注解中提取SpEL表达式，解析并生成缓存key。
     * 
     * 工作流程：
     * 1. 获取注解：从方法上获取@CacheKey注解
     * 2. 验证注解：如果注解不存在，抛出异常
     * 3. 提取表达式：从注解的value属性中提取SpEL表达式
     * 4. 验证表达式：如果表达式为空，抛出异常
     * 5. 调用generateKey：使用SpEL解析表达式并生成key
     * 
     * 异常处理：
     * - 如果方法没有@CacheKey注解，抛出IllegalArgumentException
     * - 如果表达式为空，抛出IllegalArgumentException
     * 
     * @param target 目标对象（未使用）
     * @param method 目标方法，必须标注@CacheKey注解
     * @param params 方法参数，可在SpEL表达式中引用
     * @return 生成的缓存key字符串
     * @throws IllegalArgumentException 如果方法没有@CacheKey注解或表达式为空
     */
    override fun generate(target: Any, method: Method, vararg params: Any?): Any {
        val annotation = method.getAnnotation<CacheKey>(CacheKey::class.java)
        requireNotNull(annotation) { "使用ContextKeyGenerator，需额外增加@CacheKey配置" }
        val key: String? = annotation.value
        require(!key.isNullOrBlank()) { "@CacheKey未指定规则" }

        return generateKey(method, key, *params)
    }

    /**
     * 使用SpEL表达式生成缓存key
     * 
     * 构建SpEL表达式上下文，注入KudosContext，解析表达式并生成缓存key。
     * 
     * 工作流程：
     * 1. 创建评估上下文：使用MethodBasedEvaluationContext，包含方法参数信息
     * 2. 获取上下文：从KudosContextHolder获取当前线程的KudosContext
     * 3. 注入变量：将KudosContext设置为"_context"变量
     * 4. 解析表达式：使用SpEL解析器解析cacheKey表达式
     * 5. 获取值：从表达式中获取String类型的值
     * 
     * SpEL上下文变量：
     * - #_context：KudosContext对象，包含租户ID、子系统代码、追踪键等
     * - #argName或#p0：方法参数（通过parameterNameDiscoverer获取参数名）
     * 
     * 表达式示例：
     * - "#_context.tenantId + '::' + #id"：返回"租户ID::参数id的值"
     * - "#_context.subSystemCode + ':' + #user.id"：返回"子系统代码:用户ID"
     * - "#id"：只使用参数id（不使用上下文）
     * 
     * 注意事项：
     * - cacheKey必须是有效的SpEL表达式
     * - 表达式必须返回String类型
     * - 如果表达式解析失败，会抛出异常
     * - KudosContext可能为null，表达式需要处理null情况
     * 
     * @param method 目标方法
     * @param cacheKey SpEL表达式字符串
     * @param params 方法参数
     * @return 解析后的缓存key字符串
     */
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
