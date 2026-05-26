package io.kudos.base.io

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase

/**
 * Filename utility.
 *
 * @author K
 * @since 1.0.0
 */
object FilenameKit {

    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    // Wraps org.apache.commons.io.FilenameUtils
    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

    //region normalize
    /**
     * Normalizes a file path, removing double and single dot segments.
     * This method normalizes a path. The input separators can be in either Unix or Windows format, while the output
     * separator depends on the current operating system.
     * Trailing slashes are preserved. Double slashes are merged into single slashes (but UNC names are handled).
     * Single dot segments are removed. Double dots cause this segment and the previous one to be removed.
     * If the double dot has no parent segment, null is returned.
     * The output is consistent across Unix and Windows operating systems, except for the separator.
     *
     * <pre>
     * /foo//               -->   /foo/
     * /foo/./              -->   /foo/
     * /foo/../bar          -->   /bar
     * /foo/../bar/         -->   /bar/
     * /foo/../bar/../baz   -->   /baz
     * //foo//./bar         -->   /foo/bar
     * /../                 -->   null
     * ../foo               -->   null
     * foo/bar/..           -->   foo/
     * foo/../../bar        -->   null
     * foo/../bar           -->   bar
     * //server/foo/../bar  -->   //server/bar
     * //server/../bar      -->   null
     * C:\foo\..\bar        -->   C:\bar
     * C:\..\bar            -->   null
     * ~/foo/../bar/        -->   ~/bar/
     * ~/../bar             -->   null
     * </pre>
     *
     * (Note: the output path separator depends on the current operating system.)
     *
     * @param filename the file path to normalize; null returns null
     * @return the normalized file path; null for invalid paths
     * @author K
     * @since 1.0.0
     */
    fun normalize(filename: String?): String? = FilenameUtils.normalize(filename)

    /**
     * Normalizes a file path, removing double and single dot segments. The separator to use can be specified.
     * This method normalizes a path. The input separators can be in either Unix or Windows format, and the output
     * separator is specified by the parameter.
     * Trailing slashes are preserved. Double slashes are merged into single slashes (but UNC names are handled).
     * Single dot segments are removed. Double dots cause this segment and the previous one to be removed.
     * If the double dot has no parent segment, null is returned.
     * The output is consistent across Unix and Windows operating systems, except for the separator.
     *
     * <pre>
     * /foo//               -->   /foo/
     * /foo/./              -->   /foo/
     * /foo/../bar          -->   /bar
     * /foo/../bar/         -->   /bar/
     * /foo/../bar/../baz   -->   /baz
     * //foo//./bar         -->   /foo/bar
     * /../                 -->   null
     * ../foo               -->   null
     * foo/bar/..           -->   foo/
     * foo/../../bar        -->   null
     * foo/../bar           -->   bar
     * //server/foo/../bar  -->   //server/bar
     * //server/../bar      -->   null
     * C:\foo\..\bar        -->   C:\bar
     * C:\..\bar            -->   null
     * ~/foo/../bar/        -->   ~/bar/
     * ~/../bar             -->   null
     * </pre>
     *
     * The output path is consistent across Unix and Windows operating systems.
     *
     * @param filename the file path to normalize
     * @param unixSeparator true to use Unix-style separators; false to use Windows-style separators
     * @return the normalized file path; null for invalid paths
     * @author K
     * @since 1.0.0
     */
    fun normalize(filename: String, unixSeparator: Boolean): String = FilenameUtils.normalize(filename, unixSeparator)

    /**
     * Normalizes a file path, removing double and single dot segments, and removes any trailing separator.
     * This method normalizes a path. The input separators can be in either Unix or Windows format, while the output
     * separator depends on the current operating system.
     * Trailing slashes are preserved. Double slashes are merged into single slashes (but UNC names are handled).
     * Single dot segments are removed. Double dots cause this segment and the previous one to be removed.
     * If the double dot has no parent segment, null is returned.
     * The output is consistent across Unix and Windows operating systems, except for the separator.
     *
     * <pre>
     * /foo//               -->   /foo
     * /foo/./              -->   /foo
     * /foo/../bar          -->   /bar
     * /foo/../bar/         -->   /bar
     * /foo/../bar/../baz   -->   /baz
     * //foo//./bar         -->   /foo/bar
     * /../                 -->   null
     * ../foo               -->   null
     * foo/bar/..           -->   foo
     * foo/../../bar        -->   null
     * foo/../bar           -->   bar
     * //server/foo/../bar  -->   //server/bar
     * //server/../bar      -->   null
     * C:\foo\..\bar        -->   C:\bar
     * C:\..\bar            -->   null
     * ~/foo/../bar/        -->   ~/bar
     * ~/../bar             -->   null
     * </pre>
     *
     * (Note: the output path separator depends on the current operating system.)
     *
     * @param filename the file path to normalize; null returns null
     * @return the normalized file path; null for invalid paths
     * @author K
     * @since 1.0.0
     */
    fun normalizeNoEndSeparator(filename: String?): String? = FilenameUtils.normalizeNoEndSeparator(filename)

    /**
     * Normalizes a file path, removing double and single dot segments and any trailing separator. The separator to use can be specified.
     * This method normalizes a path. The input separators can be in either Unix or Windows format, and the output
     * separator is specified by the parameter.
     * Trailing slashes are preserved. Double slashes are merged into single slashes (but UNC names are handled).
     * Single dot segments are removed. Double dots cause this segment and the previous one to be removed.
     * If the double dot has no parent segment, null is returned.
     * The output is consistent across Unix and Windows operating systems, except for the separator.
     *
     * <pre>
     * /foo//               -->   /foo
     * /foo/./              -->   /foo
     * /foo/../bar          -->   /bar
     * /foo/../bar/         -->   /bar
     * /foo/../bar/../baz   -->   /baz
     * //foo//./bar         -->   /foo/bar
     * /../                 -->   null
     * ../foo               -->   null
     * foo/bar/..           -->   foo
     * foo/../../bar        -->   null
     * foo/../bar           -->   bar
     * //server/foo/../bar  -->   //server/bar
     * //server/../bar      -->   null
     * C:\foo\..\bar        -->   C:\bar
     * C:\..\bar            -->   null
     * ~/foo/../bar/        -->   ~/bar
     * ~/../bar             -->   null
     * </pre>
     *
     * @param filename the file path to normalize; null returns null
     * @param unixSeparator true to use Unix-style separators; false to use Windows-style separators
     * @return the normalized file path; null for invalid paths
     * @author K
     * @since 1.0.0
     */
    fun normalizeNoEndSeparator(filename: String?, unixSeparator: Boolean = false): String? =
        FilenameUtils.normalizeNoEndSeparator(filename, unixSeparator)
    //endregion normalize

    /**
     * Concatenates a child path to a base path using standard command-line rules.
     * The first argument is the base path and the second is the path to concatenate. The returned path is always
     * normalized via [.normalize], so that `..` is handled correctly.
     * If the path to concatenate is absolute (has an absolute path prefix), it is normalized and returned. Otherwise,
     * the path is concatenated to the base path and returned normalized.
     * The output is consistent across Unix and Windows operating systems, except for the separator.
     *
     * <pre>
     * /foo/ + bar          -->   /foo/bar
     * /foo + bar           -->   /foo/bar
     * /foo + /bar          -->   /bar
     * /foo + C:/bar        -->   C:/bar
     * /foo + C:bar         -->   C:bar (*)
     * /foo/a/ + ../bar     -->   foo/bar
     * /foo/ + ../../bar    -->   null
     * /foo/ + /bar         -->   /bar
     * /foo/.. + /bar       -->   /bar
     * /foo + bar/c.txt     -->   /foo/bar/c.txt
     * /foo/c.txt + bar     -->   /foo/c.txt/bar (!)
     * </pre>
     *
     * Note: (*) Relative paths with a Windows drive letter are unreliable when using this method.
     * (!) The first argument must be a path; if it ends with a filename, the filename is concatenated into the result.
     * If this is a problem, use the [.getFullPath] method on the first argument.
     *
     * @param basePath the base path to concatenate to; always treated as a path
     * @param fullFilenameToAdd the filename (or path) to concatenate to the base path
     * @return the concatenated path; null for invalid paths
     * @author K
     * @since 1.0.0
     */
    fun concat(basePath: String?, fullFilenameToAdd: String?): String? =
        FilenameUtils.concat(basePath, fullFilenameToAdd)

    /**
     * Checks whether the parent directory contains the given child directory or file.
     * The filenames are normalized.
     * Edge cases:
     *  A directory does not contain itself: returns false
     *  A null file or sub-directory returns false
     *
     * @param canonicalParent the parent directory
     * @param canonicalChild the file or sub-directory
     * @return true if the parent directory contains the given child directory or file; otherwise false
     * @author K
     * @since 1.0.0
     */
    fun directoryContains(canonicalParent: String, canonicalChild: String?): Boolean =
        FilenameUtils.directoryContains(canonicalParent, canonicalChild)

    //region separatorsTo
    /**
     * Converts all separators to Unix-style separators.
     *
     * @param path the path to process; null returns null
     * @return the updated path
     * @author K
     * @since 1.0.0
     */
    fun separatorsToUnix(path: String?): String? = FilenameUtils.separatorsToUnix(path)

    /**
     * Converts all separators to Windows-style separators.
     *
     * @param path the path to process; null returns null
     * @return the updated path
     * @author K
     * @since 1.0.0
     */
    fun separatorsToWindows(path: String?): String? = FilenameUtils.separatorsToWindows(path)

    /**
     * Converts all separators to the current system separator.
     *
     * @param path the path to process; null returns null
     * @return the updated path
     * @author K
     * @since 1.0.0
     */
    fun separatorsToSystem(path: String?): String? = FilenameUtils.separatorsToSystem(path)
    //endregion separatorsTo

    /**
     * Returns the path prefix, such as `C:/` or `~/`.
     * This method handles files in either Unix or Windows format.
     * The length of the prefix includes the first slash in the full path (if applicable). Therefore, it is possible
     * for the returned length to be greater than the length of the input path.
     *
     * <pre>
     * Windows:
     * a\b\c.txt           --> ""          --> relative path
     * \a\b\c.txt          --> "\"         --> current drive absolute path
     * C:a\b\c.txt         --> "C:"        --> drive-relative path
     * C:\a\b\c.txt        --> "C:\"       --> absolute path
     * \\server\a\b\c.txt  --> "\\server\" --> UNC
     *
     * Unix:
     * a/b/c.txt           --> ""          --> relative path
     * /a/b/c.txt          --> "/"         --> absolute path
     * ~/a/b/c.txt         --> "~/"        --> current user directory
     * ~                   --> "~/"        --> current user directory (with trailing slash)
     * ~user/a/b/c.txt     --> "~user/"    --> user directory
     * ~user               --> "~user/"    --> user directory (with trailing slash)
     * </pre>
     *
     * The output is consistent across operating systems. For example, both Unix and Windows ignore prefix matching.
     *
     * @param filename the path whose prefix to find; null returns -1
     * @return the length of the prefix; -1 if the path is invalid or null
     * @author K
     * @since 1.0.0
     */
    fun getPrefixLength(filename: String?): Int = FilenameUtils.getPrefixLength(filename)

    //region indexOf
    /**
     * Returns the index of the last directory separator.
     * This method handles files in either Unix or Windows format. The index of the last slash or backslash is returned.
     * The output is consistent across operating systems.
     *
     * @param filename the path to search; null returns -1
     * @return the index of the last directory separator; -1 if none found or the path is null
     * @author K
     * @since 1.0.0
     */
    fun indexOfLastSeparator(filename: String?): Int = FilenameUtils.indexOfLastSeparator(filename)

    /**
     * Returns the index of the last extension separator (a dot).
     * This method also checks that there is no directory separator after the last dot. It uses [.indexOfLastSeparator]
     * for this, which handles files in either Unix or Windows format.
     * The output is consistent across operating systems.
     *
     * @param filename the path to search; null returns -1
     * @return the index of the last extension separator; -1 if none found or the path is null
     * @author K
     * @since 1.0.0
     */
    fun indexOfExtension(filename: String?): Int = FilenameUtils.indexOfExtension(filename)
    //endregion indexOf

    /**
     * Returns the prefix from a full path, such as `C:/` or `~/`.
     * This method handles files in either Unix or Windows format. The first slash in the full path (if any) is included in the returned prefix.
     *
     * <pre>
     * Windows:
     * a\b\c.txt           --> ""          --> relative path
     * \a\b\c.txt          --> "\"         --> current drive absolute path
     * C:a\b\c.txt         --> "C:"        --> drive-relative path
     * C:\a\b\c.txt        --> "C:\"       --> absolute path
     * \\server\a\b\c.txt  --> "\\server\" --> UNC
     *
     * Unix:
     * a/b/c.txt           --> ""          --> relative path
     * /a/b/c.txt          --> "/"         --> absolute path
     * ~/a/b/c.txt         --> "~/"        --> current user directory
     * ~                   --> "~/"        --> current user directory (with trailing slash)
     * //	 * ~user/a/b/c.txt     --> "~user/"    --> user directory
     * ~user               --> "~user/"    --> user directory (with trailing slash)
     * </pre>
     *
     *  The output is consistent across operating systems. For example, both Unix and Windows ignore prefix matching.
     *
     * @param filename the path to search; null returns null
     * @return the prefix of the path
     * @author K
     * @since 1.0.0
     */
    fun getPrefix(filename: String): String? = FilenameUtils.getPrefix(filename)

    /**
     * Returns the extension of a filename.
     * This method returns the text after the dot symbol in the filename. There must be no directory separator after that dot.
     *
     * <pre>
     * foo.txt      --> "txt"
     * a/b/c.jpg    --> "jpg"
     * a/b.txt/c    --> ""
     * a/b/c        --> ""
     * </pre>
     *
     * The output is consistent across operating systems.
     *
     * @param filename the filename to get the extension of
     * @return the extension of the file; an empty string if none
     * @author K
     * @since 1.0.0
     */
    fun getExtension(filename: String): String = FilenameUtils.getExtension(filename)

    //region getPath
    /**
     * Returns the path of a full path excluding the prefix.
     * This method handles files in either Unix or Windows format. It is purely text-based and returns the text up to (and including) the last slash or backslash.
     *
     *  <pre>
     * C:\a\b\c.txt --> a\b\
     * ~/a/b/c.txt  --> a/b/
     * a.txt        --> ""
     * a/b/c        --> a/b/
     * a/b/c/       --> a/b/c/
     * </pre>
     *
     * The output is consistent across operating systems.
     * This method discards the prefix from the result. To retain the prefix, see the [.getFullPath] method.
     *
     * @param filename the path to search; null returns null
     * @return the path excluding the prefix; an empty string if none; null if the path is invalid or null
     * @author K
     * @since 1.0.0
     */
    fun getPath(filename: String?): String? = FilenameUtils.getPath(filename)

    /**
     * Returns the path of a full path excluding the prefix, and also excludes any trailing directory separator.
     * This method handles files in either Unix or Windows format. It is purely text-based and returns the text up to (and including) the last slash or backslash.
     *
     * <pre>
     * C:\a\b\c.txt --> a\b
     * ~/a/b/c.txt  --> a/b
     * a.txt        --> ""
     * a/b/c        --> a/b
     * a/b/c/       --> a/b/c
     * </pre>
     *
     * The output is consistent across operating systems.
     * This method discards the prefix from the result. To retain the prefix, see the [.getFullPathNoEndSeparator] method.
     *
     * @param filename the path to search; null returns null
     * @return the path excluding the prefix; an empty string if none; null if the path is invalid or null
     * @author K
     * @since 1.0.0
     */
    fun getPathNoEndSeparator(filename: String?): String? = FilenameUtils.getPathNoEndSeparator(filename)

    /**
     * Returns the full path of the given file, including the prefix and the path.
     * This method handles files in either Unix or Windows format. It is purely text-based and returns the text up to (and including) the last slash or backslash.
     *
     * <pre>
     * C:\a\b\c.txt --> C:\a\b\
     * ~/a/b/c.txt  --> ~/a/b/
     * a.txt        --> ""
     * a/b/c        --> a/b/
     * a/b/c/       --> a/b/c/
     * C:           --> C:
     * C:\          --> C:\
     * ~            --> ~/
     * ~/           --> ~/
     * ~user        --> ~user/
     * ~user/       --> ~user/
     * </pre>
     *
     * The output is consistent across operating systems.
     *
     * @param filename the path to search; null returns null
     * @return the path excluding the prefix; an empty string if none; null if the path is invalid or null
     * @author K
     * @since 1.0.0
     */
    fun getFullPath(filename: String?): String? = FilenameUtils.getFullPath(filename)

    /**
     * Returns the full path of the given file, including the prefix and the path, and excludes the trailing directory separator.
     * This method handles files in either Unix or Windows format. It is purely text-based and returns the text up to (and including) the last slash or backslash.
     *
     * <pre>
     * C:\a\b\c.txt --> C:\a\b
     * ~/a/b/c.txt  --> ~/a/b
     * a.txt        --> ""
     * a/b/c        --> a/b
     * a/b/c/       --> a/b/c
     * C:           --> C:
     * C:\          --> C:\
     * ~            --> ~
     * ~/           --> ~
     * ~user        --> ~user
     * ~user/       --> ~user
     * </pre>
     *
     * The output is consistent across operating systems.
     *
     * @param filename the path to search; null returns null
     * @return the path including the prefix; an empty string if none; null if the path is invalid or null
     * @author K
     * @since 1.0.0
     */
    fun getFullPathNoEndSeparator(filename: String?): String? = FilenameUtils.getFullPathNoEndSeparator(filename)
    //endregion getPath

    //region getName
    /**
     * Returns the name from a full filename, after removing the path.
     * This method handles files in either Unix or Windows format. It is purely text-based and returns the text after the last slash or backslash.
     *
     * <pre>
     * a/b/c.txt --> c.txt
     * a.txt     --> a.txt
     * a/b/c     --> c
     * a/b/c/    --> ""
     * </pre>
     *
     * The output is consistent across operating systems.
     *
     * @param filename the path to search; null returns null
     * @return the filename after removing the path; an empty string if none; null if the path is invalid or null
     * @author K
     * @since 1.0.0
     */
    fun getName(filename: String?): String? = FilenameUtils.getName(filename)

    /**
     * Returns the name from a full filename, after removing the path and extension.
     * This method handles files in either Unix or Windows format. The text after the last slash or backslash, and before the last dot, is returned.
     *
     * <pre>
     * a/b/c.txt --> c
     * a.txt     --> a
     * a/b/c     --> c
     * a/b/c/    --> ""
     * </pre>
     *
     * The output is consistent across operating systems.
     *
     * @param filename the path to search; null returns null
     * @return the filename after removing the path and extension; an empty string if none; null if the path is invalid or null
     * @author K
     * @since 1.0.0
     */
    fun getBaseName(filename: String?): String? = FilenameUtils.getBaseName(filename)
    //endregion getName

    /**
     * Removes the extension.
     * This method returns the text before the dot in the filename. There must be no directory separator after that dot.
     *
     * <pre>
     * foo.txt    --> foo
     * a\b\c.jpg  --> a\b\c
     * a\b\c      --> a\b\c
     * a.b\c      --> a.b\c
     * </pre>
     *
     * The output is consistent across operating systems.
     *
     * @param filename the path to search; null returns null
     * @return the filename without the extension; null if the path is null
     * @author K
     * @since 1.0.0
     */
    fun removeExtension(filename: String?): String? = FilenameUtils.removeExtension(filename)

    //region equals
    /**
     * Checks whether two filenames are exactly equal.
     * This method does no processing of the two filenames apart from the comparison; it is simply a null-safe, case-sensitive equals operation.
     *
     * @param filename1 the first filename to compare; may be null
     * @param filename2 the second filename to compare; may be null
     * @return true if the two filenames are equal; both null are considered equal
     * @see IOCase.SENSITIVE
     * @author K
     * @since 1.0.0
     */
    fun equals(filename1: String?, filename2: String?): Boolean = FilenameUtils.equals(filename1, filename2)

    /**
     * Checks whether two filenames are equal, depending on the case rules of the operating system.
     * This method does no processing of the two filenames apart from the comparison. Comparison is case-sensitive on Unix and case-insensitive on Windows.
     *
     *  @param filename1 the first filename to compare; may be null
     * @param filename2 the second filename to compare; may be null
     * @return true if the two filenames are equal; both null are considered equal
     * @see IOCase.SYSTEM
     * @author K
     * @since 1.0.0
     */
    fun equalsOnSystem(filename1: String?, filename2: String?): Boolean =
        FilenameUtils.equalsOnSystem(filename1, filename2)

    /**
     * Normalizes two filenames and then compares them for equality.
     * Both filenames are first processed by [.normalize], then compared in a case-sensitive manner.
     *
     * @param filename1 the first filename to compare; may be null
     * @param filename2 the second filename to compare; may be null
     * @return true if the two filenames are equal; both null are considered equal
     * @see IOCase.SENSITIVE
     * @author K
     * @since 1.0.0
     */
    fun equalsNormalized(filename1: String?, filename2: String?): Boolean =
        FilenameUtils.equalsNormalized(filename1, filename2)

    /**
     * Normalizes two filenames and then compares them for equality, depending on the case rules of the operating system.
     * Both filenames are first processed by [.normalize], then compared.
     * Comparison is case-sensitive on Unix and case-insensitive on Windows.
     *
     * @param filename1 the first filename to compare; may be null
     * @param filename2 the second filename to compare; may be null
     * @return true if the two filenames are equal; both null are considered equal
     * @see IOCase.SYSTEM
     * @author K
     * @since 1.0.0
     */
    fun equalsNormalizedOnSystem(filename1: String?, filename2: String?): Boolean =
        FilenameUtils.equalsNormalizedOnSystem(filename1, filename2)

    /**
     * Checks whether two filenames are equal, optionally normalizing them and choosing the case-comparison rule.
     *
     * @param filename1 the first filename to compare; may be null
     * @param filename2 the second filename to compare; may be null
     * @param normalized whether to normalize the filenames
     * @param caseSensitivity the case-comparison rule; null depends on the system
     * @return true if the two filenames are equal; both null are considered equal
     * @author K
     * @since 1.0.0
     */
    fun equals(filename1: String?, filename2: String?, normalized: Boolean, caseSensitivity: Boolean?): Boolean =
        FilenameUtils.equals(filename1, filename2, normalized, adaptCaseSensitivity(caseSensitivity))
    //endregion equals

    //region isExtension
    /**
     * Checks whether the file's extension matches the given extension.
     * This method treats the text after the "." in the filename as the extension. There must be no directory separator after the ".".
     * The extension check is case-sensitive on all platforms.
     *
     * @param filename the filename to check; may be null
     * @param extension the extension; null or empty string checks for no extension
     * @return true if the filename's extension matches the given extension
     * @author K
     * @since 1.0.0
     */
    fun isExtension(filename: String?, extension: String?): Boolean = FilenameUtils.isExtension(filename, extension)

    /**
     * Checks whether the file's extension matches one of the given extensions.
     * This method treats the text after the "." in the filename as the extension. There must be no directory separator after the ".".
     * The extension check is case-sensitive on all platforms.
     *
     * @param filename the filename to check; may be null
     * @param extensions the array of extensions; null or empty string checks for no extension
     * @return true if the filename's extension matches one of the given extensions
     * @author K
     * @since 1.0.0
     */
    fun isExtension(filename: String?, extensions: Array<String?>): Boolean =
        FilenameUtils.isExtension(filename, *extensions)

    /**
     * Checks whether the file's extension matches one of the given extensions.
     * This method treats the text after the "." in the filename as the extension. There must be no directory separator after the ".".
     * The extension check is case-sensitive on all platforms.
     *
     * @param filename the filename to check; may be null
     * @param extensions the collection of extensions; null or empty string checks for no extension
     * @return true if the filename's extension matches one of the given extensions
     * @author K
     * @since 1.0.0
     */
    fun isExtension(filename: String?, extensions: Collection<String?>?): Boolean =
        FilenameUtils.isExtension(filename, extensions)
    //endregion isExtension

    //region wildcard
    /**
     * <p>
     * Determines whether a filename matches the given string, which may contain wildcards. Case-sensitive.
     * </p>
     *
     * <p>
     * Use '?' and '*' to represent a single character and multiple characters (zero or more) respectively. This works
     * the same way as the Dos/Unix command line. The check is always case-sensitive.
     * </p>
     *
     * <pre>
     * wildcardMatch("c.txt", "*.txt")      --> true
     * wildcardMatch("c.txt", "*.jpg")      --> false
     * wildcardMatch("a/b/c.txt", "a/b\*")  --> true
     * wildcardMatch("c.txt", "*.???")      --> true
     * wildcardMatch("c.txt", "*.????")     --> false
     * </pre>
     *
     * Note: "*?" sequences do not work correctly in the current string comparison.
     *
     * @param filename the filename to check; may be null
     * @param wildcardMatcher the string with wildcards; may be null
     * @return true if it matches; both null are considered a match
     * @see IOCase#SENSITIVE
     * @author K
     * @since 1.0.0
     */
    fun wildcardMatch(filename: String?, wildcardMatcher: String?): Boolean =
        FilenameUtils.wildcardMatch(filename, wildcardMatcher)

    /**
     * <p>
     * Determines whether a filename matches the given string, which may contain wildcards. The case sensitivity depends on the current system.
     * </p>
     *
     * <p>
     * Use '?' and '*' to represent a single character and multiple characters (zero or more) respectively. This works the same way as the Dos/Unix command line.
     * Comparison is case-sensitive on Unix and case-insensitive on Windows.
     * </p>
     *
     * <pre>
     * wildcardMatch("c.txt", "*.txt")      --> true
     * wildcardMatch("c.txt", "*.jpg")      --> false
     * wildcardMatch("a/b/c.txt", "a/b\*")  --> true
     * wildcardMatch("c.txt", "*.???")      --> true
     * wildcardMatch("c.txt", "*.????")     --> false
     * </pre>
     *
     * Note: "*?" sequences do not work correctly in the current string comparison.
     *
     * @param filename the filename to check; may be null
     * @param wildcardMatcher the string with wildcards; may be null
     * @return true if it matches; both null are considered a match
     * @see IOCase#SYSTEM
     * @author K
     * @since 1.0.0
     */
    fun wildcardMatchOnSystem(filename: String?, wildcardMatcher: String?): Boolean =
        FilenameUtils.wildcardMatchOnSystem(filename, wildcardMatcher)

    /**
     * Determines whether a filename matches the given string, which may contain wildcards. The case-sensitivity rule can be specified.
     * Use '?' and '*' to represent a single character and multiple characters (zero or more) respectively. Note: "*?" sequences do not work correctly in the current string comparison.
     *
     * @param filename the filename to check; may be null
     * @param wildcardMatcher the string with wildcards; may be null
     * @param caseSensitivity the case-comparison rule; null depends on the system
     * @return true if it matches; both null are considered a match
     * @author K
     * @since 1.0.0
     */
    fun wildcardMatch(filename: String?, wildcardMatcher: String?, caseSensitivity: Boolean?): Boolean =
        FilenameUtils.wildcardMatch(filename, wildcardMatcher, adaptCaseSensitivity(caseSensitivity))
    //endregion wildcard

    private fun adaptCaseSensitivity(caseSensitivity: Boolean?): IOCase =
        when (caseSensitivity) {
            null -> IOCase.SYSTEM
            true -> IOCase.SENSITIVE
            false -> IOCase.INSENSITIVE
        }

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // Wraps org.apache.commons.io.FilenameUtils
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

}
