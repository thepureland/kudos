package io.kudos.ability.file.minio.init.properties

/**
 * MinIO client configuration, corresponding to `kudos.ability.file.minio.*`.
 *
 * @property endpoint internal MinIO address (e.g. service DNS or IP inside a K8s cluster)
 * @property accessKey AK — used by the static client
 * @property secretKey SK — used by the static client
 * @property publicEndpoint externally accessible MinIO address, returned to business
 *   code as [io.kudos.ability.file.common.entity.UploadFileResult.pathPrefix] so
 *   the frontend can directly concatenate `pathPrefix + filePath` into an accessible URL.
 *   Usually differs from [endpoint] — internal traffic goes through service DNS,
 *   external traffic via ingress domain / CDN.
 * @property partSize multipart part size passed to the MinIO SDK when uploading
 *   streams of unknown size; defaults to 10 MiB.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class MinioProperties {
    var endpoint: String? = null
    var accessKey: String? = null
    var secretKey: String? = null
    var publicEndpoint: String? = null
    var partSize: Long = DEFAULT_PART_SIZE
        set(value) {
            require(value >= MIN_PART_SIZE) { "partSize must be greater than or equal to $MIN_PART_SIZE" }
            field = value
        }

    companion object {
        const val DEFAULT_PART_SIZE: Long = 10L * 1024L * 1024L
        const val MIN_PART_SIZE: Long = 5L * 1024L * 1024L
    }
}
