package io.kudos.tools.codegen.core.merge

import io.kudos.base.io.FileKit
import java.io.File
import java.util.regex.Matcher

/**
 * Code merger: merges previously generated code with the freshly generated code.
 *
 * @author K
 * @since 1.0.0
 */
class CodeMerger(private val file: File) {

    /** Old file content captured before overwrite; serves as the "previously generated" baseline,
     *  from which user-written code and historic imports are extracted */
    private val oldFileContent: String = FileKit.readFileToString(file)
    /** Continually-accumulated new file content during merging; flushed to disk by [merge]'s final
     *  [FileKit.write] call */
    private lateinit var newFileContent: CharSequence
    /** Parser for user-written code inside `//region your codes XXX` blocks */
    private val retriever: UserCodesRetriever = UserCodesRetriever(oldFileContent)

    /**
     * Performs the merge after generation.
     */
    fun merge() {
        handleRegion()
        handleImport()
        FileKit.write(file, newFileContent)
    }

    /**
     * Merges the user-written code between `//region your codes N` ... `//endregion your codes N`.
     *
     * Flow:
     * 1. Extract per-region user code from the old file (key = region index).
     * 2. Read the freshly generated file as the draft.
     * 3. If the template declared "code to append to this region" via `AppendCodesRetriever`,
     *    use [AppendCodeType] to decide whether to append the whole block or line-by-line
     *    (with per-line deduplication in PARTIBLE mode).
     * 4. Replace the body of each region with "user code + appended code" using a regex; the regex
     *    is also compatible with the HTML/JSP comment style `<!--//region-->`.
     *
     * @author K
     * @since 1.0.0
     */
    private fun handleRegion() {
        val customCodes = retriever.retrieve()
        newFileContent = FileKit.readFileToString(file)
        val appendCodesMap = AppendCodesRetriever(newFileContent).retrieve()
        for ((index, value) in customCodes) {
            val codes = StringBuilder(value)
            appendCodesMap[index]?.let { (type, appendCodes) ->
                if (type == AppendCodeType.PARTIBLE) {
                    appendCodes.split("\n")
                        .filter { codes.indexOf(it) == -1 }
                        .forEach { codes.append(it).append("\n") }
                    codes.append("\n")
                } else if (codes.indexOf(appendCodes) == -1) {
                    codes.append(appendCodes).append("\n")
                }
            }
            val regexp =
                "(?<=(<!--)?#?//region your codes $index(-->)?\\r?\\n)[\\s\\S]*?(?=(<!--)?#?//endregion your codes $index(-->)?)"
            newFileContent = newFileContent.replaceFirst(regexp.toRegex(), Matcher.quoteReplacement(codes.toString()))
        }
    }

    /**
     * Merges import statements (applies only to `.kt` files).
     *
     * Old imports minus their intersection with new imports = the user's own added imports.
     * That set is inserted back before the first `import` line of the new file, preserving the
     * user's extra dependencies without duplicating imports already in the template.
     *
     * @author K
     * @since 1.0.0
     */
    private fun handleImport() {
        if (!file.name.endsWith(".kt")) return
        val oldImports = ImportStmtRetriever(oldFileContent).retrieveImports()
        val newImports = ImportStmtRetriever(newFileContent).retrieveImports()
        // The set difference is the user's own imports
        val customImport = oldImports.subtract(newImports.toSet())
        if (customImport.isEmpty()) return
        val imports = customImport.joinToString(separator = "", postfix = "") { "$it\n" }
        val index = newFileContent.indexOf("import")
        newFileContent = StringBuilder(newFileContent).insert(index, imports).toString()
    }
}