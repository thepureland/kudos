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
import org.springframework.stereotype.Component

/**
 * Feign缓存请求拦截器
 * 
 * 在Feign请求中添加客户端缓存相关的请求头，实现服务间缓存的协商机制。
 * 
 * 核心功能：
 * 1. 缓存key生成：根据请求URL、方法、请求体生成唯一的缓存key
 * 2. 本地缓存查询：查询本地缓存，获取缓存数据的UUID
 * 3. 请求头设置：将缓存key和UUID设置到请求头，供服务端判断
 * 
 * 工作流程：
 * 1. 检查本地缓存：如果客户端没有本地缓存，直接返回
 * 2. 生成缓存key：根据请求信息生成MD5哈希的缓存key
 * 3. 设置缓存key头：将缓存key设置到请求头
 * 4. 查询本地缓存：从本地缓存中查询数据
 * 5. 设置缓存UID头：如果找到缓存数据，将UUID设置到请求头
 * 
 * 缓存协商：
 * - 服务端收到请求后，会比较请求头中的UID和服务端数据的UID
 * - 如果UID相同，返回304状态码，客户端使用本地缓存
 * - 如果UID不同，返回新数据和新的UID，客户端更新缓存
 * 
 * Key生成规则：
 * - 包含租户ID、应用名称、请求URL、HTTP方法、请求体
 * - 使用MD5加密生成固定长度的key
 * - 确保相同请求生成相同的key
 * 
 * 注意事项：
 * - 只有在客户端有本地缓存时才添加请求头
 * - 缓存key包含租户信息，确保多租户隔离
 * - UUID用于判断缓存是否过期
 */
@Component
class FeignCacheRequestInterceptor : RequestInterceptor {

    @Autowired
    @Qualifier("feignCacheHelper")
    private lateinit var cacheHelper: ClientCacheHelper

    @Value($$"${spring.application.name}")
    private val applicationName: String? = null

    /**
     * 应用请求拦截，添加缓存相关的请求头
     * 
     * 在Feign请求中添加缓存key和缓存UID，实现客户端缓存协商。
     * 
     * 工作流程：
     * 1. 检查本地缓存：如果客户端没有本地缓存，直接返回
     * 2. 生成缓存key：调用genCacheKey生成请求的唯一标识
     * 3. 设置缓存key头：将key设置到请求头，供服务端识别
     * 4. 查询本地缓存：从本地缓存中查询数据
     * 5. 设置缓存UID头：如果找到缓存数据，将UUID设置到请求头
     * 
     * 请求头说明：
     * - HEADER_KEY_CACHE_KEY：缓存key，用于服务端识别请求
     * - HEADER_KEY_CACHE_UID：缓存UUID，用于判断缓存是否过期
     * 
     * 缓存协商：
     * - 服务端比较请求头中的UID和当前数据的UID
     * - 如果相同，返回304，客户端使用本地缓存
     * - 如果不同，返回新数据和新的UID
     * 
     * @param requestTemplate Feign请求模板
     */
    override fun apply(requestTemplate: RequestTemplate) {
        if (!cacheHelper.havaLocalCache()) {
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
     * 根据请求信息生成唯一的缓存key，用于标识和查找缓存数据。
     * 
     * 工作流程：
     * 1. 获取请求信息：提取URL、HTTP方法、请求体
     * 2. 获取租户ID：从KudosContext中获取当前租户ID
     * 3. 构建key字符串：拼接分隔符、租户ID、应用名称、请求信息
     * 4. MD5加密：使用Md5Crypt.apr1Crypt生成固定长度的哈希值
     * 
     * Key组成：
     * - 分隔符 + 租户ID + 分隔符 + 应用名称 + 请求信息
     * - 请求信息包括：URL、HTTP方法、请求体
     * 
     * 加密方式：
     * - 使用MD5加密（apr1格式）
     * - 固定salt："fCache"
     * - 生成固定长度的哈希值
     * 
     * 唯一性保证：
     * - 包含租户ID，确保多租户隔离
     * - 包含应用名称，区分不同服务
     * - 包含请求信息，区分不同请求
     * - MD5加密确保key的唯一性和固定长度
     * 
     * 注意事项：
     * - 相同请求会生成相同的key
     * - 不同租户的相同请求会生成不同的key
     * - key长度固定，便于存储和比较
     * 
     * @param requestTemplate Feign请求模板
     * @return MD5加密后的缓存key
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
