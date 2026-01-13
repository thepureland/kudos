package io.kudos.ability.cache.interservice.aop

import io.kudos.ability.cache.interservice.common.ClientCacheItem
import io.kudos.ability.cache.interservice.common.ClientCacheKey
import io.kudos.ability.cache.interservice.provider.web.CacheClientRequest
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import java.io.Serializable

/**
 * 客户端缓存切面
 * 
 * 通过AOP方式实现服务间缓存功能，支持基于请求的缓存命中判断和数据返回。
 * 
 * 核心功能：
 * 1. 缓存命中判断：通过比较请求头中的cache-uid和响应结果的uid来判断缓存是否命中
 * 2. 缓存状态通知：通过HTTP响应头告知客户端缓存使用状态
 *    - STATUS_USE_CACHE(304)：缓存命中，客户端应使用本地缓存
 *    - STATUS_DO_CACHE(200)：缓存未命中，返回新数据，客户端应缓存
 * 3. 结果UID生成：基于方法返回结果生成唯一标识（MD5哈希），用于缓存一致性判断
 * 
 * 工作流程：
 * - 拦截标注@ClientCacheable的Controller方法
 * - 执行方法获取返回结果
 * - 从请求头获取客户端传递的cache-uid
 * - 计算返回结果的uid（基于类名和JSON序列化内容）
 * - 比较请求uid和结果uid：
 *   - 相同：返回null，设置响应头为304，告知客户端使用缓存
 *   - 不同：返回结果，设置响应头为200，告知客户端缓存新数据
 * 
 * 注意事项：
 * - 仅支持在Controller类的方法上使用
 * - 需要配合CacheClientRequest使用，普通HTTP请求不会触发缓存逻辑
 * - 结果对象必须可序列化为JSON，用于生成uid
 */
@Aspect
@Lazy(false)
@Order(0)
class ClientCacheableAspect {
    /**
     * 定义切入点
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.cache.interservice.aop.ClientCacheable)")
    private fun cut() {
    }

    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        validateClass(joinPoint)
        //执行方法，得到结果
        val result = doProceed(joinPoint)
        if (result == null) {
            return null
        }
        //获取RequestAttributes
        val requestAttributes = RequestContextHolder.getRequestAttributes()!!
        val request = requestAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST) as HttpServletRequest
        if (request is CacheClientRequest) {
            //客户端数据的uid
            val reqUid: String? = request.getHeader(ClientCacheKey.HEADER_KEY_CACHE_UID)
            //服务端结果的uid
            val resUid: String = ClientCacheItem.genUid(result)
            request.getServletResponse()!!.setHeader(ClientCacheKey.HEADER_KEY_CACHE_UID, resUid)
            request.getServletResponse()!!
                .setHeader(ClientCacheKey.HEADER_KEY_CACHE_STATUS, ClientCacheKey.STATUS_DO_CACHE)
            if (reqUid.isNullOrBlank()) {
                return result
            }
            //判断请求uid和结果uid
            if (resUid == reqUid) {
                //判断结果的uid与请求uid是否一致，一致则告知客户端直接使用缓存（304），并返回空
                request.getServletResponse()!!
                    .setHeader(ClientCacheKey.HEADER_KEY_CACHE_STATUS, ClientCacheKey.STATUS_USE_CACHE)
                return null
            }
            return result
        } else {
            return result
        }
    }

    /**
     * 执行方法
     *
     * @param joinPoint joinPoint
     * @return result
     */
    private fun doProceed(joinPoint: ProceedingJoinPoint): Any? {
        try {
            return joinPoint.proceed()
        } catch (throwable: Throwable) {
            throw RuntimeException(throwable)
        }
    }

    /**
     * 验证class是否为controller
     *
     * @param joinPoint
     */
    private fun validateClass(joinPoint: ProceedingJoinPoint) {
        val clazz: Class<*> = joinPoint.target.javaClass
        val restController = clazz.getAnnotation(RestController::class.java)
        val controller = clazz.getAnnotation(Controller::class.java)
        check(!(restController == null && controller == null)) {
            "类${clazz.getName()}必须是Controller才可使用ClientCacheable！"
        }
    }

}
