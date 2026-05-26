package io.kudos.tools.codegen.core.merge

/**
 * Import-statement retriever.
 *
 * @author K
 * @since 1.0.0
 */
class ImportStmtRetriever(private val fileContent: CharSequence) {
    fun retrieveImports(): List<String> =
        IMPORT_REGEX.findAll(fileContent).map { it.value }.toList()

    private companion object {
        private val IMPORT_REGEX = Regex("import .+")
    }
}
