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
        val requestAttributes: RequestAttributes = RequestContextHolder.getRequestAttributes()
        val request: HttpServletRequest =
            requestAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST) as HttpServletRequest
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
