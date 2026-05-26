package io.kudos.context.core

import io.kudos.base.model.contract.entity.IIdEntity

/**
 * Kudos context.
 *
 * @author K
 * @since 1.0.0
 */
class KudosContext {

    companion object {
        /** Standard key for "specified data source" in [otherInfos] */
        const val OTHER_INFO_KEY_DATA_SOURCE = "_DATA_SOURCE_"
        /** Standard key for "specified database" in [otherInfos] */
        const val OTHER_INFO_KEY_DATABASE = "_DATABASE_"
        /** Standard key for "verification code" in [otherInfos] */
        const val OTHER_INFO_KEY_VERIFY_CODE = "_VERIFY_CODE_"
        /** Standard key for "current user" in [sessionAttributes] */
        const val SESSION_KEY_USER = "_USER_"
    }

    /** Portal code */
    var portalCode: String? = null

    /** Subsystem code */
    var subSystemCode: String? = null

    /** Microservice code */
    var microServiceCode: String? = null

    /** Atomic service code */
    var atomicServiceCode: String? = null

    /** Tenant id */
    var tenantId: String? = null

    /** Data source id; when null, it is determined by the routing strategy */
    var dataSourceId: String? = null

    /** Read-only (standby) data source id */
    var readOnlyDataSourceId: String? = null

    /** Internal: data-source-level tenant id (distinct from [tenantId]; the former is used for sharding routing) */
    var _datasourceTenantId: String? = null

    /** User */
    var user: IIdEntity<String>? = null

    /** Log trace keyword string; the format can be customized */
    var traceKey: String? = null

    /** Client information object */
    var clientInfo: ClientInfo? = null

    /** Session attribute information */
    var sessionAttributes: MutableMap<String, Any?>? = null

    /** Cookie attribute information */
    var cookieAttributes: MutableMap<String, String?>? = null

    /** Header attribute information */
    var headerAttributes: MutableMap<String, String?>? = null

    /** Other information */
    var otherInfos: MutableMap<String, Any?>? = null

    /**
     * Append Session attributes.
     * Lazily creates the underlying [MutableMap] on the first call, avoiding an empty context permanently holding an
     * empty collection.
     *
     * @param sessionAttributes Key-value pairs to append
     * @return The current [KudosContext] itself, for chaining
     * @author K
     * @since 1.0.0
     */
    fun addSessionAttributes(vararg sessionAttributes: Pair<String, Any?>): KudosContext {
        val attrs = this.sessionAttributes ?: mutableMapOf<String, Any?>().also { this.sessionAttributes = it }
        attrs.putAll(sessionAttributes)
        return this
    }

    /**
     * Append Cookie attributes.
     * Lazily creates the underlying [MutableMap] on the first call.
     *
     * @param cookieAttributes Key-value pairs to append
     * @return The current [KudosContext] itself, for chaining
     * @author K
     * @since 1.0.0
     */
    fun addCookieAttributes(vararg cookieAttributes: Pair<String, String?>): KudosContext {
        val attrs = this.cookieAttributes ?: mutableMapOf<String, String?>().also { this.cookieAttributes = it }
        attrs.putAll(cookieAttributes)
        return this
    }

    /**
     * Append Header attributes.
     * Lazily creates the underlying [MutableMap] on the first call.
     *
     * @param headerAttributes Key-value pairs to append
     * @return The current [KudosContext] itself, for chaining
     * @author K
     * @since 1.0.0
     */
    fun addHeaderAttributes(vararg headerAttributes: Pair<String, String?>): KudosContext {
        val attrs = this.headerAttributes ?: mutableMapOf<String, String?>().also { this.headerAttributes = it }
        attrs.putAll(headerAttributes)
        return this
    }

    /**
     * Append other extension information.
     * Lazily creates the underlying [MutableMap] on the first call; see [OTHER_INFO_KEY_DATA_SOURCE] and friends for
     * standard keys.
     *
     * @param otherInfos Key-value pairs to append
     * @return The current [KudosContext] itself, for chaining
     * @author K
     * @since 1.0.0
     */
    fun addOtherInfos(vararg otherInfos: Pair<String, Any?>): KudosContext {
        val infos = this.otherInfos ?: mutableMapOf<String, Any?>().also { this.otherInfos = it }
        infos.putAll(otherInfos)
        return this
    }

}