package io.kudos.ability.comm.sms.aliyun.model

import java.io.Serial
import java.io.Serializable

/**
 * Aliyun SMS send request body.
 *
 * Fields correspond to the input parameters of the Aliyun SDK `SendSmsRequest`; it also carries
 * the credentials ([accessKeyId] / [accessKeySecret]) used for the call. Credentials are passed
 * along with the request rather than placed in a config file in order to support the
 * "multi-tenant, each with its own AK" scenario.
 *
 * **Do not log instances of this class** - `accessKeySecret` is plaintext; [Serializable] allows
 * the object to be serialized to logs / caches and leak the secret.
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
class AliyunSmsRequest : Serializable {
    /**
     * Region.
     */
    var region: String? = null

    /**
     * accessKeyId.
     */
    var accessKeyId: String? = null

    /**
     * accessKeySecret.
     */
    var accessKeySecret: String? = null

    /**
     * Phone number(s) to receive the SMS. Phone number format:
     * Domestic SMS: +/+86/0086/86 or an 11-digit phone number without any prefix, e.g. 1390000****
     * International / HK / Macau / Taiwan messages: international area code + number, e.g. 852000012****
     * Supports sending SMS to multiple phone numbers, separated by half-width commas (,). Limit is
     * 1000 phone numbers.
     * Batch calls have slightly higher latency compared to single calls.
     */
    var phoneNumbers: String? = null

    /**
     * SMS signature name.
     */
    var signName: String? = null

    /**
     * SMS template CODE.
     */
    var templateCode: String? = null

    /**
     * Actual values corresponding to the SMS template variables.
     * Supports passing multiple parameters, e.g.: {"name":"Zhang San","number":"1390000****"}
     */
    var templateParam: String? = null

    companion object {
        @Serial
        private const val serialVersionUID = 31845461795590341L
    }
}
