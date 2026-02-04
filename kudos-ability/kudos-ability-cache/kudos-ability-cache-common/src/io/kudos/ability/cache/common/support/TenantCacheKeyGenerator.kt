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
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.ParameterNameDiscoverer
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import java.lang.reflect.Method
import java.util.*

/**
 * 租户缓存key生成器
 * 
 * 为TenantCacheable等注解生成包含租户ID的缓存key，实现多租户隔离。
 * 
 * 核心功能：
 * 1. 自动添加租户ID：在所有生成的key前自动添加租户ID前缀
 * 2. SpEL表达式支持：支持使用SpEL表达式动态生成key
 * 3. 注解提取：从方法注解中提取key配置（suffix属性）
 * 4. 上下文变量：提供method、tenantId等变量供SpEL使用
 * 
 * Key格式：
 * - 如果指定了suffix："{tenantId}::{suffix表达式结果}"
 * - 如果未指定suffix："{tenantId}"
 * 
 * 支持的注解：
 * - TenantCacheable、TenantCacheEvict、TenantCachePut
 * - CachePut、Cacheable、CacheEvict（兼容标准注解）
 * 
 * SpEL表达式：
 * - 支持引用方法参数（#argName或#p0）
 * - 支持引用方法对象（#method）
 * - 支持引用租户ID（#tenantId）
 * - 支持引用目标对象（#root.target）
 * 
 * 注意事项：
 * - 租户ID从KudosContext中获取
 * - 如果suffix为空，只返回租户ID
 * - 使用SpEL表达式解析，支持复杂的key生成逻辑
 */
class TenantCacheKeyGenerator : KeyGenerator {
    private val parameterNameDiscoverer: ParameterNameDiscoverer = DefaultParameterNameDiscoverer()

    private val parser: ExpressionParser = SpelExpressionParser()

    /**
     * 生成普通key（带租户ID）
     * 
     * 根据提供的key字符串生成包含租户ID的缓存key。
     * 
     * 工作流程：
     * 1. 检查key：如果key不为空，拼接租户ID和key
     * 2. 如果key为空：只返回租户ID
     * 3. 调用generateKey：使用SpEL表达式解析并生成最终key
     * 
     * Key拼接规则：
     * - 如果key不为空：使用"#tenantId.concat('::').concat($key)"表达式
     * - 如果key为空：使用"#tenantId"表达式
     * 
     * @param target 目标对象
     * @param method 目标方法
     * @param key 原始的key字符串（SpEL表达式）
     * @param params 方法参数
     * @return 生成的缓存key（包含租户ID）
     */
    fun generalNormalKey(target: Any, method: Method, key: String, vararg params: Any?): Any {
        var key = key
        if (key.isNotBlank()) {
            key = "#tenantId.concat('::').concat($key)"
        } else {
            key = "#tenantId"
        }
        return generateKey(target, method, key, *params)
    }

    /**
     * 生成缓存key（实现KeyGenerator接口）
     * 
     * 从方法注解中提取key配置，生成包含租户ID的缓存key。
     * 
     * 工作流程：
     * 1. 提取注解key：从方法注解中提取suffix属性
     * 2. 拼接租户ID：如果suffix不为空，拼接租户ID和suffix
     * 3. 调用generateKey：使用SpEL表达式解析并生成最终key
     * 
     * 注解查找顺序：
     * - TenantCacheable、TenantCacheEvict、TenantCachePut
     * - CachePut、Cacheable、CacheEvict
     * - 找到第一个有suffix的注解即使用
     * 
     * @param target 目标对象
     * @param method 目标方法
     * @param params 方法参数
     * @return 生成的缓存key（包含租户ID）
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
     * 从方法注解中提取key配置
     * 
     * 遍历支持的注解类型，查找第一个有suffix属性的注解并返回其值。
     * 
     * 查找顺序：
     * 1. TenantCacheable
     * 2. TenantCacheEvict
     * 3. TenantCachePut
     * 4. CachePut
     * 5. Cacheable
     * 6. CacheEvict
     * 
     * 返回值：
     * - 如果找到有suffix的注解，返回suffix值
     * - 如果所有注解都没有suffix或不存在，返回空字符串
     * 
     * @param method 目标方法
     * @return 从注解中提取的key配置（suffix值），如果不存在则返回空字符串
     */
    private fun getAnnotationKey(method: Method): String {
        return KEY_ANNOTATIONS.stream()
            .map { type: Class<out Annotation?>? -> AnnotationUtils.findAnnotation(method, type) }
            .filter { obj: Any? -> Objects.nonNull(obj) }
            .map<String> { ann: Annotation? -> AnnotationUtils.getValue(ann, "suffix") as String? }
            .filter { obj: String? -> Objects.nonNull(obj) }
            .findFirst()
            .orElse("")
    }

    /**
     * 使用SpEL表达式生成缓存key
     * 
     * 构建SpEL表达式上下文，解析cacheKey表达式并生成最终的缓存key。
     * 
     * 工作流程：
     * 1. 创建评估上下文：使用MethodBasedEvaluationContext，包含方法参数信息
     * 2. 设置根对象：将target设置为根对象
     * 3. 设置变量：
     *    - method：目标方法对象
     *    - tenantId：从KudosContext获取的租户ID
     * 4. 解析表达式：使用SpEL解析器解析cacheKey表达式
     * 5. 获取值：从表达式中获取String类型的值
     * 
     * SpEL上下文变量：
     * - #method：目标方法对象
     * - #tenantId：当前租户ID
     * - #root.target：目标对象
     * - #argName或#p0：方法参数（通过parameterNameDiscoverer获取参数名）
     * 
     * 表达式示例：
     * - "#tenantId"：只返回租户ID
     * - "#tenantId.concat('::').concat(#id)"：返回"租户ID::参数id的值"
     * - "#tenantId + '::' + #user.id"：返回"租户ID::用户ID"
     * 
     * 注意事项：
     * - cacheKey必须是有效的SpEL表达式
     * - 表达式必须返回String类型
     * - 如果表达式解析失败，会抛出异常
     * 
     * @param target 目标对象
     * @param method 目标方法
     * @param cacheKey SpEL表达式字符串
     * @param params 方法参数
     * @return 解析后的缓存key字符串
     */
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
