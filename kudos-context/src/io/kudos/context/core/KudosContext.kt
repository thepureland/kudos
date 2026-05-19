package io.kudos.context.core

import io.kudos.base.model.contract.entity.IIdEntity

/**
 * Kudos上下文
 *
 * @author K
 * @since 1.0.0
 */
class KudosContext {

    companion object {
        /** [otherInfos] 中"指定数据源"的标准 key */
        const val OTHER_INFO_KEY_DATA_SOURCE = "_DATA_SOURCE_"
        /** [otherInfos] 中"指定数据库"的标准 key */
        const val OTHER_INFO_KEY_DATABASE = "_DATABASE_"
        /** [otherInfos] 中"验证码"的标准 key */
        const val OTHER_INFO_KEY_VERIFY_CODE = "_VERIFY_CODE_"
        /** [sessionAttributes] 中"当前用户"的标准 key */
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

    /** 内部：数据源粒度的租户 id（与 [tenantId] 区分，前者用于分库分表路由） */
    var _datasourceTenantId: String? = null

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

    /**
     * 追加 Session 属性。
     * 首次调用时惰性创建底层 [MutableMap]，避免空上下文一直持有空集合。
     *
     * @param sessionAttributes 待追加的键值对
     * @return 当前 [KudosContext] 自身，便于链式调用
     * @author K
     * @since 1.0.0
     */
    fun addSessionAttributes(vararg sessionAttributes: Pair<String, Any?>): KudosContext {
        val attrs = this.sessionAttributes ?: mutableMapOf<String, Any?>().also { this.sessionAttributes = it }
        attrs.putAll(sessionAttributes)
        return this
    }

    /**
     * 追加 Cookie 属性。
     * 首次调用时惰性创建底层 [MutableMap]。
     *
     * @param cookieAttributes 待追加的键值对
     * @return 当前 [KudosContext] 自身，便于链式调用
     * @author K
     * @since 1.0.0
     */
    fun addCookieAttributes(vararg cookieAttributes: Pair<String, String?>): KudosContext {
        val attrs = this.cookieAttributes ?: mutableMapOf<String, String?>().also { this.cookieAttributes = it }
        attrs.putAll(cookieAttributes)
        return this
    }

    /**
     * 追加 Header 属性。
     * 首次调用时惰性创建底层 [MutableMap]。
     *
     * @param headerAttributes 待追加的键值对
     * @return 当前 [KudosContext] 自身，便于链式调用
     * @author K
     * @since 1.0.0
     */
    fun addHeaderAttributes(vararg headerAttributes: Pair<String, String?>): KudosContext {
        val attrs = this.headerAttributes ?: mutableMapOf<String, String?>().also { this.headerAttributes = it }
        attrs.putAll(headerAttributes)
        return this
    }

    /**
     * 追加其它扩展信息。
     * 首次调用时惰性创建底层 [MutableMap]；标准 key 见 [OTHER_INFO_KEY_DATA_SOURCE] 等。
     *
     * @param otherInfos 待追加的键值对
     * @return 当前 [KudosContext] 自身，便于链式调用
     * @author K
     * @since 1.0.0
     */
    fun addOtherInfos(vararg otherInfos: Pair<String, Any?>): KudosContext {
        val infos = this.otherInfos ?: mutableMapOf<String, Any?>().also { this.otherInfos = it }
        infos.putAll(otherInfos)
        return this
    }

}