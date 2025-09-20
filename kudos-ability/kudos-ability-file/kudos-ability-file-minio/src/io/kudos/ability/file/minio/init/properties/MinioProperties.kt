package io.kudos.ability.file.minio.init.properties

class MinioProperties {
    /**
     * 连接url
     */
    var endpoint: String? = null

    /**
     * 用户名
     */
    var accessKey: String? = null

    /**
     * 密码
     */
    var secretKey: String? = null

    /**
     * 外网访问的minio地址
     */
    var publicEndpoint: String? = null
}
