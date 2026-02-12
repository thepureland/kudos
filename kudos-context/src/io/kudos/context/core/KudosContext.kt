package io.kudos.context.core

import io.kudos.base.support.IIdEntity


/**
 * Kudos上下文
 *
 * @author K
 * @since 1.0.0
 */
class KudosContext {

    companion object {
        const val OTHER_INFO_KEY_DATA_SOURCE = "_DATA_SOURCE_"
        const val OTHER_INFO_KEY_DATABASE = "_DATABASE_"
        const val OTHER_INFO_KEY_VERIFY_CODE = "_VERIFY_CODE_"
        const val SESSION_KEY_USER = "_USER_"
    }

    /** 门户编码 */
    var portalCode: String? = null

    /** 子系统编码 */
    var subSystemCode: String? = null

    /** 微服务编码 */
    var microServiceCode: String? = null

    /** 原子服务编码 */
    var atomicServiceCode: String? = null

    /** 租户id */
    var tenantId: String? = null

    /** 数据源id，为null将根据路由策略决定 */
    var dataSourceId: String? = null

    /** 备库数据源id */
    var readOnlyDataSourceId: String? = null

    var _datasourceTenantId: String? = null

//    var userId: String? = null

    /** 用户 */
    var user: IIdEntity<String>? = null

    /** 日志跟踪关键词串，格式可自定义 */
    var traceKey: String? = null

    /** 客户端信息对象 */
    var clientInfo: ClientInfo? = null

    /** Session属性信息 */
    var sessionAttributes: MutableMap<String, Any?>? = null

    /** Cookie属性信息 */
    var cookieAttributes: MutableMap<String, String?>? = null

    /** Header属性信息 */
    var headerAttributes: MutableMap<String, String?>? = null

    /** 其他信息 */
    var otherInfos: MutableMap<String, Any?>? = null

    fun addSessionAttributes(vararg sessionAttributes: Pair<String, Any?>): KudosContext {
        val attrs = this.sessionAttributes ?: mutableMapOf<String, Any?>().also { this.sessionAttributes = it }
        attrs.putAll(mapOf(*sessionAttributes))
        return this
    }

    fun addCookieAttributes(vararg cookieAttributes: Pair<String, String?>): KudosContext {
        val attrs = this.cookieAttributes ?: mutableMapOf<String, String?>().also { this.cookieAttributes = it }
        attrs.putAll(mapOf(*cookieAttributes))
        return this
    }

    fun addHeaderAttributes(vararg headerAttributes: Pair<String, String?>): KudosContext {
        val attrs = this.headerAttributes ?: mutableMapOf<String, String?>().also { this.headerAttributes = it }
        attrs.putAll(mapOf(*headerAttributes))
        return this
    }

    fun addOtherInfos(vararg otherInfos: Pair<String, Any?>): KudosContext {
        val infos = this.otherInfos ?: mutableMapOf<String, Any?>().also { this.otherInfos = it }
        infos.putAll(mapOf(*otherInfos))
        return this
    }

}