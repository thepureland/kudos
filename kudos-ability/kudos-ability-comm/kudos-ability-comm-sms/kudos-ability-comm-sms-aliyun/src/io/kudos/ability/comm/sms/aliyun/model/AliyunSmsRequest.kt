package io.kudos.ability.comm.sms.aliyun.model

import java.io.Serial
import java.io.Serializable

/**
 * 阿里云短信发送请求体。
 *
 * 字段对应阿里云 SDK `SendSmsRequest` 的入参；同时携带本次调用使用的凭证（[accessKeyId] /
 * [accessKeySecret]）—— 凭证不放配置文件而是随请求一起传，是为了支持"多租户各自一套 AK"的场景。
 *
 * **不要把本类实例输出到日志** —— `accessKeySecret` 是明文，[Serializable] 让对象被序列化到日志 / 缓存时密钥外泄。
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
class AliyunSmsRequest : Serializable {
    /**
     * 区域
     */
    var region: String? = null

    /**
     * accessKeyId
     */
    var accessKeyId: String? = null

    /**
     * accessKeySecret
     */
    var accessKeySecret: String? = null

    /**
     * 接收短信的手机号码。手机号码格式：
     * 国内短信：+/+86/0086/86或无任何前缀的11位手机号码，例如1390000****
     * 国际/港澳台消息：国际区号+号码，例如852000012****
     * 支持对多个手机号码发送短信，手机号码之间以半角逗号（,）分隔。上限为1000个手机号码
     * 批量调用相对于单条调用及时性稍有延迟
     */
    var phoneNumbers: String? = null

    /**
     * 短信签名名称
     */
    var signName: String? = null

    /**
     * 短信模板CODE
     */
    var templateCode: String? = null

    /**
     * 短信模板变量对应的实际值
     * 支持传入多个参数，示例：{"name":"张三","number":"1390000****"}
     */
    var templateParam: String? = null

    companion object {
        @Serial
        private const val serialVersionUID = 31845461795590341L
    }
}
