package io.kudos.ability.cache.common.support

import io.kudos.context.core.KudosContextHolder
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.expression.TypedValue
import org.springframework.expression.spel.support.StandardEvaluationContext
import java.lang.reflect.Method

/**
 * Context-aware cache key generator.
 *
 * Generates cache keys for the @CacheKey annotation, supporting the use of KudosContext within SpEL expressions.
 *
 * Core responsibilities:
 * 1. Annotation-driven: reads the SpEL expression from the @CacheKey annotation.
 * 2. Context injection: injects KudosContext into the SpEL expression as a variable named "_context".
 * 3. Parameter support: allows referencing method parameters within the expression.
 *
 * Requirements:
 * - The method must be annotated with @CacheKey.
 * - The @CacheKey value attribute must provide a SpEL expression.
 * - The expression must return a String.
 *
 * SpEL expression support:
 * - Method parameters: reference with #argName or #p0.
 * - Context info: access KudosContext via #_context.
 *   - #_context.tenantId: tenant ID.
 *   - #_context.subSystemCode: subsystem code.
 *   - #_context.traceKey: trace key.
 *   - Other KudosContext properties.
 *
 * Use cases:
 * - Generating cache keys from context info.
 * - Combining method parameters with context info.
 * - Dynamically generating complex cache keys.
 *
 * Notes:
 * - @CacheKey is required; otherwise an exception is thrown.
 * - The expression must not be empty; otherwise an exception is thrown.
 * - Expression parsing failures will throw an exception.
 */
class ContextKeyGenerator : KeyGenerator {

    /** SpEL parameter name discoverer so expressions can reference method formal parameters via `#paramName`. */
    private val parameterNameDiscoverer = SpelExpressionCache.parameterNameDiscoverer

    /**
     * Generates the cache key (implements the KeyGenerator interface).
     *
     * Extracts the SpEL expression from the @CacheKey annotation, parses it, and produces the cache key.
     *
     * Workflow:
     * 1. Fetch the annotation: read @CacheKey from the method.
     * 2. Validate the annotation: throw if missing.
     * 3. Extract the expression: read the value attribute.
     * 4. Validate the expression: throw if empty.
     * 5. Invoke generateKey: parse the expression with SpEL and produce the key.
     *
     * Exception handling:
     * - If the method has no @CacheKey annotation, throws IllegalArgumentException.
     * - If the expression is empty, throws IllegalArgumentException.
     *
     * @param target target object (unused)
     * @param method target method; must be annotated with @CacheKey
     * @param params method parameters; may be referenced from the SpEL expression
     * @return the generated cache key string
     * @throws IllegalArgumentException if the method has no @CacheKey annotation or the expression is empty
     */
    override fun generate(target: Any, method: Method, vararg params: Any?): Any {
        val annotation = method.getAnnotation(CacheKey::class.java)
        requireNotNull(annotation) { "ContextKeyGenerator requires an additional @CacheKey configuration." }
        val key: String = annotation.value
        require(key.isNotBlank()) { "@CacheKey did not specify a rule." }

        return generateKey(method, key, *params)
    }

    /**
     * Generates a cache key using a SpEL expression.
     *
     * Builds the SpEL evaluation context, injects KudosContext, parses the expression, and produces the cache key.
     *
     * Workflow:
     * 1. Build the evaluation context: use MethodBasedEvaluationContext, which carries method parameter info.
     * 2. Fetch the context: obtain the current thread's KudosContext from KudosContextHolder.
     * 3. Inject the variable: set KudosContext as the "_context" variable.
     * 4. Parse the expression: use the SpEL parser on the cacheKey expression.
     * 5. Read the value: get the value as a String from the expression.
     *
     * SpEL context variables:
     * - #_context: the KudosContext object, including tenant ID, subsystem code, trace key, etc.
     * - #argName or #p0: method parameters (parameter names resolved via parameterNameDiscoverer).
     *
     * Expression examples:
     * - "#_context.tenantId + '::' + #id": returns "<tenantId>::<id>".
     * - "#_context.subSystemCode + ':' + #user.id": returns "<subSystemCode>:<userId>".
     * - "#id": uses only the id parameter (context not used).
     *
     * Notes:
     * - cacheKey must be a valid SpEL expression.
     * - The expression must return a String.
     * - Parsing failures throw an exception.
     * - KudosContext may be null; expressions should handle this case.
     *
     * @param method target method
     * @param cacheKey SpEL expression string
     * @param params method parameters
     * @return the resolved cache key string
     */
    private fun generateKey(method: Method, cacheKey: String, vararg params: Any?): String {
        val context: StandardEvaluationContext =
            MethodBasedEvaluationContext(TypedValue.NULL, method, params, parameterNameDiscoverer)
        val kudosContext = KudosContextHolder.get()
//        kudosContext.subSysCode = ApplicationInfo.getInstance().getSubSysCode()
        context.setVariable("_context", kudosContext)
        return SpelExpressionCache.get(cacheKey).getValue(context, String::class.java)!!
    }
}
