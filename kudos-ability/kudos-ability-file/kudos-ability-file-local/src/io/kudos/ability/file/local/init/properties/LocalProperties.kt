package io.kudos.ability.file.local.init.properties

/**
 * Local file storage configuration; corresponds to `kudos.ability.file.local.*`.
 *
 * @property basePath Absolute path to the file storage root directory. All buckets / files reside under this directory.
 *   When left empty / misconfigured, upload / download / delete will fail due to missing directory. For production
 *   deployment it is strongly recommended to set this to a dedicated mount point.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class LocalProperties {
    var basePath: String? = null
}
