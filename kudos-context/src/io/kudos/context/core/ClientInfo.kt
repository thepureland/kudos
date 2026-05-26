package io.kudos.context.core

import java.util.Locale
import java.util.TimeZone

/**
 * Client information.
 *
 * @author K
 * @since 1.0.0
 */
class ClientInfo(builder: Builder) {

    /** Request IP */
    var ip: String? = null

    /** Requested domain */
    var domain: String? = null

    /** Requested URL */
    var url: String? = null

    /** Request parameters */
    var params: Map<String, Array<String?>?>? = null

//    /** Byte representation of request content */
//    var requestContent: ByteArray? = null
//
    /** String representation of request content */
    var requestContentString: String? = null

    /** Request referer */
    var requestReferer: String? = null

    /** Request type (GET/POST, etc.) */
    var requestType: String? = null

    /** Client operating system */
    var os: Pair<String, String>? = null

    /** Client browser */
    var browser: Pair<String, String>? = null

    /** Client region-language */
    var locale: Locale? = null

    /** Client time zone */
    var timeZone: TimeZone? = null


    init {
        ip = builder.ip
        domain = builder.domain
        url = builder.url
        params = builder.params
//        requestContent = builder.requestContent
        requestContentString = builder.requestContentString
        requestReferer = builder.requestReferer
        requestType = builder.requestType
        os = builder.os
        browser = builder.browser
        locale = builder.locale
        timeZone = builder.timeZone
    }

    /**
     * Builder for the client information object.
     *
     * @author K
     * @since 1.0.0
     */
    class Builder {

        /** Request IP */
        internal var ip: String? = null

        /** Requested domain */
        internal var domain: String? = null

        /** Requested URL */
        internal var url: String? = null

        /** Request parameters */
        internal var params: Map<String, Array<String?>?>? = null

//        /** Byte representation of request content */
//        internal var requestContent: ByteArray? = null

        /** String representation of request content */
        internal var requestContentString: String? = null

        /** Request referer */
        internal var requestReferer: String? = null

        /** Request type (GET/POST, etc.) */
        internal var requestType: String? = null

        /** Client operating system */
        internal var os: Pair<String, String>? = null

        /** Client browser */
        internal var browser: Pair<String, String>? = null

        /** Client region-language */
        internal var locale: Locale? = null

        /** Client time zone */
        internal var timeZone: TimeZone? = null


        fun build(): ClientInfo = ClientInfo(this)


        fun ip(ip: String?): Builder {
            this.ip = ip
            return this
        }

        fun domain(domain: String?): Builder {
            this.domain = domain
            return this
        }

        fun url(url: String?): Builder {
            this.url = url
            return this
        }

        fun params(params: Map<String, Array<String?>?>?): Builder {
            this.params = params
            return this
        }

//        fun requestContent(requestContent: ByteArray?): Builder {
//            this.requestContent = requestContent
//            return this
//        }

        fun requestContentString(requestContentString: String?): Builder {
            this.requestContentString = requestContentString
            return this
        }

        fun requestReferer(requestReferer: String?): Builder {
            this.requestReferer = requestReferer
            return this
        }

        fun requestType(requestType: String?): Builder {
            this.requestType = requestType
            return this
        }

        fun os(os: Pair<String, String>?): Builder {
            this.os = os
            return this
        }

        fun browser(browser: Pair<String, String>?): Builder {
            this.browser = browser
            return this
        }

        fun locale(locale: Locale?): Builder {
            this.locale = locale
            return this
        }

        fun timeZone(timeZone: TimeZone?): Builder {
            this.timeZone = timeZone
            return this
        }

    }

}