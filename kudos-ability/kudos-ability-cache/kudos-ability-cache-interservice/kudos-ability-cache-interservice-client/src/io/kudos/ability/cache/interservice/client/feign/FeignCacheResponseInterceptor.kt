package io.kudos.ability.cache.interservice.client.feign

import feign.FeignException
import feign.Response
import io.kudos.ability.cache.interservice.client.core.ClientCacheHelper
import io.kudos.ability.cache.interservice.common.ClientCacheItem
import io.kudos.ability.cache.interservice.common.ClientCacheKey
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer
import org.springframework.cloud.openfeign.support.SpringDecoder
import java.io.IOException
import java.lang.reflect.Type

/**
 * Feign 请求返回拦截器：
 * 当请求为304时，直接获取本地缓存结果
 */
class FeignCacheResponseInterceptor(
    messageConverters: ObjectFactory<HttpMessageConverters>,
    customizers: ObjectProvider<HttpMessageConverterCustomizer>
) : SpringDecoder(messageConverters, customizers) {

    @Autowired
    private val cacheHelper: ClientCacheHelper? = null

    @Throws(IOException::class, FeignException::class)
    override fun decode(response: Response, type: Type?): Any? {
        if (!cacheHelper!!.havaLocalCache()) {
            return super.decode(response, type)
        }
        val headers = response.headers()
        //如果header没有cache-id或没有cache-status
        if (!headers.containsKey(ClientCacheKey.HEADER_KEY_CACHE_UID) || !headers.containsKey(ClientCacheKey.HEADER_KEY_CACHE_STATUS)) {
            return super.decode(response, type)
        }
        var result: Any?
        //获取response的cache-status
        val status = headers[ClientCacheKey.HEADER_KEY_CACHE_STATUS]
        val cacheStatus = status!!.first()
        //获取本次请求的本地缓存key：url+method+params
        val cacheKeys = response.request().headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY]
        val cacheKey = cacheKeys!!.first()
        log.info("缓存Key：$cacheKey")
        log.info("服务返回状态：$cacheStatus")
        if (ClientCacheKey.STATUS_USE_CACHE == cacheStatus) {
            //header头部状态为304,则从本地缓存获取数据并返回
            val cacheItem = cacheHelper.loadFromLocalCache(cacheKey)
            result = cacheItem!!.cacheData
        } else {
            //否则，将本次数据放入本地缓存
            result = super.decode(response, type)
            //获取缓存对象的uid
            val cacheUids = headers[ClientCacheKey.HEADER_KEY_CACHE_UID]
            val cacheUid = cacheUids!!.first()
            val cacheItem = ClientCacheItem(cacheUid, result)
            cacheHelper.writeToLocalCache(cacheKey, cacheItem)
        }
        return result
    }

    private val log = LogFactory.getLog(this)

}
