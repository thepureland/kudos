package io.kudos.ability.file.minio.init.properties

/**
 * MinIO 客户端配置，对应 `kudos.ability.file.minio.*`。
 *
 * @property endpoint 内部访问的 MinIO 地址（如 K8s 集群内 service DNS 或 IP）
 * @property accessKey AK——静态客户端用
 * @property secretKey SK——静态客户端用
 * @property publicEndpoint 外网访问的 MinIO 地址，作为 [io.kudos.ability.file.common.entity.UploadFileResult.pathPrefix]
 *   返回给业务，让前端直接拼接 `pathPrefix + filePath` 形成可访问 URL。
 *   通常和 [endpoint] 不同——内部走 service DNS、外部走 ingress 域名 / CDN。
 *
 * @author K
 * @since 1.0.0
 */
class MinioProperties {
    var endpoint: String? = null
    var accessKey: String? = null
    var secretKey: String? = null
    var publicEndpoint: String? = null
}
