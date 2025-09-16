package io.kudos.ability.cache.interservice.client.feign

import feign.RequestInterceptor
import feign.RequestTemplate
import io.kudos.ability.cache.interservice.client.core.ClientCacheHelper
import io.kudos.ability.cache.interservice.common.ClientCacheKey
import io.kudos.context.core.KudosContextHolder
import org.apache.commons.codec.digest.Md5Crypt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value

/**
 * feign请求拦截
 * 解析请求的客户端参数，获取本地缓存的hash值，设置到header中
 */
class FeignCacheRequestInterceptor : RequestInterceptor {
    @Autowired
    @Qualifier("feignCacheHelper")
    private val cacheHelper: ClientCacheHelper? = null

    @Value("\${spring.application.name}")
    private val applicationName: String? = null

    override fun apply(requestTemplate: RequestTemplate) {
        if (!cacheHelper!!.havaLocalCache()) {
            return
        }
        val localCacheKey = genCacheKey(requestTemplate)
        //拼装本次请求的key值
        requestTemplate.header(ClientCacheKey.HEADER_KEY_CACHE_KEY, localCacheKey)
        val data = cacheHelper.loadFromLocalCache(localCacheKey!!)
        if (data != null) {
            //去除本次请求的uid值
            requestTemplate.header(ClientCacheKey.HEADER_KEY_CACHE_UID, data.uuid)
        }
    }

    /**
     * 生成本地缓存key
     *
     * @param requestTemplate requestTemplate
     * @return localCacheKey
     */
    private fun genCacheKey(requestTemplate: RequestTemplate): String? {
        val request = requestTemplate.request()
        var tenantId: String? = ""
        val tempTenantId = KudosContextHolder.get().tenantId
        if (tempTenantId != null) {
            tenantId = tempTenantId
        }
        val feignCacheKey = ClientCacheKey(request.url(), request.httpMethod().name, request.body())
        val result = arrayOf(
            ClientCacheKey.FEIGN_CACHE_DELIMITER,
            tenantId,
            ClientCacheKey.FEIGN_CACHE_DELIMITER,
            applicationName,
            feignCacheKey.toString()
        ).joinToString()
        return Md5Crypt.apr1Crypt(result, "fCache")
    }
}
