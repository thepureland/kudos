package io.kudos.ability.cache.common.aop

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature
import org.aspectj.lang.reflect.MethodSignature
import org.aspectj.lang.reflect.SourceLocation
import org.aspectj.runtime.internal.AroundClosure
import java.lang.reflect.Method

/**
 * 测试用的轻量 AOP 假件：手工构造 [ProceedingJoinPoint] / [MethodSignature]，
 * 让各切面的 around 逻辑可以脱离 Spring AOP 代理直接被调用，从而精确控制
 * proceed() 的返回值与调用次数，覆盖代理方式难以触达的分支。
 *
 * @author K
 * @since 1.0.0
 */
internal class FakeMethodSignature(private val method: Method) : MethodSignature {
    override fun getMethod(): Method = method
    override fun getReturnType(): Class<*> = method.returnType
    override fun getName(): String = method.name
    override fun getModifiers(): Int = method.modifiers
    override fun getDeclaringType(): Class<*> = method.declaringClass
    override fun getDeclaringTypeName(): String = method.declaringClass.name
    override fun getParameterTypes(): Array<Class<*>> = method.parameterTypes
    override fun getParameterNames(): Array<String> = emptyArray()
    override fun getExceptionTypes(): Array<Class<*>> = method.exceptionTypes
    override fun toShortString(): String = method.name
    override fun toLongString(): String = method.toGenericString()
    override fun toString(): String = method.toString()
}

/**
 * 假 [ProceedingJoinPoint]：proceed() 委托给传入的 lambda，并记录调用次数。
 *
 * @author K
 * @since 1.0.0
 */
internal class FakeProceedingJoinPoint(
    private val method: Method,
    private val argArray: Array<Any?>,
    private val targetObj: Any,
    private val proceedFn: () -> Any?
) : ProceedingJoinPoint {

    /** proceed 被调用的次数（验证“是否走了原方法”）。 */
    var proceedCount = 0
        private set

    override fun proceed(): Any? {
        proceedCount++
        return proceedFn()
    }

    override fun proceed(args: Array<out Any?>?): Any? {
        proceedCount++
        return proceedFn()
    }

    override fun `set$AroundClosure`(arc: AroundClosure?) {
        // no-op
    }

    override fun getSignature(): Signature = FakeMethodSignature(method)
    override fun getArgs(): Array<Any?> = argArray
    override fun getTarget(): Any = targetObj
    override fun getThis(): Any = targetObj
    override fun getKind(): String = "method-execution"
    override fun getSourceLocation(): SourceLocation = throw UnsupportedOperationException("not needed in tests")
    override fun getStaticPart(): JoinPoint.StaticPart = throw UnsupportedOperationException("not needed in tests")
    override fun toShortString(): String = method.name
    override fun toLongString(): String = method.toGenericString()
}

/** 按名称取声明方法（测试辅助）。 */
internal fun methodOf(clazz: Class<*>, name: String): Method =
    clazz.declaredMethods.first { it.name == name }
