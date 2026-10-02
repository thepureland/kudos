package io.kudos.ms.sys.common.i18n.vo.request

/** Message paths are later expanded into browser objects, so prototype names are reserved. */
object I18nPathPolicy {
    const val SAFE_PATH_PATTERN = "^(?!(?:[\\s\\S]*\\.)?(?:__proto__|constructor|prototype)(?:\\.|$))[^.]+(?:\\.[^.]+)*$"
    private val safePath = Regex(SAFE_PATH_PATTERN)

    fun requireSafe(path: String) {
        require(safePath.matches(path)) { "Invalid or reserved i18n message path" }
    }
}
