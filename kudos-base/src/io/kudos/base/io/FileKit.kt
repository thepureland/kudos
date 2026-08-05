package io.kudos.base.io

import io.kudos.base.logger.LogFactory
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.apache.commons.io.FileUtils
import org.apache.commons.io.LineIterator
import org.apache.commons.io.filefilter.IOFileFilter
import java.io.*
import java.math.BigInteger
import java.net.URL
import java.util.jar.JarFile
import java.util.zip.Checksum

/**
 * File utility.
 *
 * @author K
 * @since 1.0.0
 */
object FileKit {

    /**
     * Filename prefix used by this utility when generating temporary files.
     *
     * @author K
     * @since 1.0.0
     */
    const val PREFIX_TEMP_FILE: String = "FileKit_"

    /** Logger. */
    private val log = LogFactory.getLog(this::class)

    /**
     * Lists resource names (including files and directories) under a given directory inside a jar file.
     *
     * @param jarPath jar path (e.g. jar:file:/path/to/xxx.jar!)
     * @param dirInJar directory inside the jar to query (e.g. "assets")
     * @param recursive whether to recurse into all sub-directories; when false, only the immediate level is searched
     * @param includeDirs whether to include directory names (true: include directories, false: return files only)
     * @return list of resource (or directory) names
     */
    fun listFilesOrDirsInJar(
        jarPath: String,
        dirInJar: String,
        recursive: Boolean = false,
        includeDirs: Boolean = true
    ): List<String> {
        val jarFilePath = jarPath.removePrefix("jar:file:").substringBefore("!")
        val result = mutableSetOf<String>()
        val normalizedDir = dirInJar.removePrefix("/").trimEnd('/') + "/"
        JarFile(jarFilePath).use { jarFile ->
            val entries = jarFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name

                if (name == normalizedDir) continue
                if (!name.startsWith(normalizedDir)) continue

                val relative = name.removePrefix(normalizedDir)

                if (!recursive && relative.contains("/")) {
                    // Keep only direct sub-directories or files at the first level
                    val firstSegment = relative.substringBefore("/")
                    val fullPath = "$normalizedDir$firstSegment/"
                    if (includeDirs) result.add(fullPath)
                } else {
                    // Recursive or file
                    if (entry.isDirectory && includeDirs) {
                        result.add(name)
                    } else if (!entry.isDirectory) {
                        result.add(name)
                    }
                }
            }
        }
        return result.toList().distinct().sorted()
    }

    /**
     * Compresses a single file into a zip archive and optionally encrypts it.
     *
     * @param file the file entity
     * @param fileName the complete name of the file inside the archive; if blank, defaults to file.name
     * @param password the encryption password in plain text; if blank, the archive is not encrypted
     * @return the zip archive File object (stored in the system temp directory; call file.delete() when done)
     * @author K
     * @since 1.0.0
     */
    fun zip(file: File, fileName: String?, password: String?): File? {
        // Create a temporary zip file
        val zipFile = File.createTempFile("zip_temp_", ".zip")
        val nameInZip = fileName?.takeIf { it.isNotBlank() } ?: file.name
        val encrypted = !password.isNullOrBlank()
        val params = ZipParameters().apply {
            fileNameInZip = nameInZip
            if (encrypted) {
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            }
        }
        return runCatching {
            val zip = if (encrypted) ZipFile(zipFile, password.toCharArray()) else ZipFile(zipFile)
            zip.addFile(file, params)
            zipFile
        }.getOrElse {
            // IOException / RuntimeException were previously caught separately doing the same thing; unified via runCatching
            log.error(it)
            zipFile.delete()
            null
        }
    }


    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    // Wraps org.apache.commons.io.FileUtils
    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

    /**
     * Constructs a File object from the given parent directory and name set.
     *
     * @param directory the parent directory
     * @param names variable-length array of names
     * @return the file object
     * @author K
     * @since 1.0.0
     */
    fun getFile(directory: File, vararg names: String): File = FileUtils.getFile(directory, *names)

    /**
     * Constructs a File object from the given name set.
     *
     * @param names variable-length array of names
     * @return the file object
     * @author K
     * @since 1.0.0
     */
    fun getFile(vararg names: String): File = FileUtils.getFile(*names)

    /**
     * Opens a [FileInputStream] for the given file, providing better error messages than simply calling
     * `new FileInputStream(file)`.
     * At the end of the method, either the stream is opened successfully, or an exception is thrown.
     *
     * @param file the file to open as an input stream
     * @return a new {@link FileInputStream} for the given file
     * @throws FileNotFoundException if the file does not exist
     * @throws IOException if the file exists but is a directory
     * @throws IOException if the file exists but cannot be read
     * @author K
     * @since 1.0.0
     */
    fun openInputStream(file: File): FileInputStream = FileUtils.openInputStream(file)

    /**
     * Opens a [FileOutputStream] for the given file, checking and creating the parent directory if it does not exist.
     *
     * At the end of the method, either the stream is opened successfully, or an exception is thrown.
     * The parent directory will be created if it does not exist. The file will also be created if it does not exist.
     * If the file exists but is a directory, an IOException is thrown.
     * If the file exists but cannot be written, an IOException is thrown.
     * If the parent directory cannot be created, an IOException is thrown.
     *
     * @param file the file to open as an output stream
     * @param append `true` if bytes should be appended to the end of the file rather than overwriting it
     * @return a new {@link FileOutputStream} for the given file
     * @author K
     * @since 1.0.0
     */
    fun openOutputStream(file: File, append: Boolean = false): FileOutputStream =
        FileUtils.openOutputStream(file, append)

    /**
     * Returns a human-readable version of the file size, where the input parameter represents the number of bytes.
     * If the size exceeds 1 GB, the whole GB count is returned.
     * That is, the size is rounded down to the nearest GB boundary.
     * Similarly for the 1 MB and 1 KB boundaries.
     *
     * @param size the number of bytes
     * @return a human-readable size (units include EB, PB, TB, GB, MB, KB, and bytes)
     * @see [IO-226 -should the rounding be changed?](https://issues.apache.org/jira/browse/IO-226)
     * @author K
     * @since 1.0.0
     */
    fun byteCountToDisplaySize(size: BigInteger): String = FileUtils.byteCountToDisplaySize(size)

    /**
     * Returns a human-readable version of the file size, where the input parameter represents the number of bytes.
     * If the size exceeds 1 GB, the whole GB count is returned.
     * That is, the size is rounded down to the nearest GB boundary.
     * Similarly for the 1 MB and 1 KB boundaries.
     *
     * @param size the number of bytes
     * @return a human-readable size (units include EB, PB, TB, GB, MB, KB, and bytes)
     * @see [IO-226 -should the rounding be changed?](https://issues.apache.org/jira/browse/IO-226)
     * @author K
     * @since 1.0.0
     */
    fun byteCountToDisplaySize(size: Long): String = FileUtils.byteCountToDisplaySize(size)

    /**
     * Implements the same behavior as Unix "touch". It creates a new file of size 0, or, if the specified file
     * already exists, opens and closes it without modification, but updates the file date and time.
     *
     * @param file the file to process
     * @throws IOException if the creation fails
     * @author K
     * @since 1.0.0
     */
    fun touch(file: File): Unit = FileUtils.touch(file)

    /**
     * Converts a collection of File instances to an array. This is mainly done to handle the
     * difference in return types between File.listFiles() and FileKit.listFiles().
     *
     * @param files collection of File instances
     * @return an array of File instances
     * @author K
     * @since 1.0.0
     */
    fun convertFileCollectionToFileArray(files: Collection<File>): Array<File> =
        FileUtils.convertFileCollectionToFileArray(files)

    /**
     * Finds files under the given directory and its sub-directories. All found files are filtered by an IOFileFilter.
     *
     * If you need to recursively search sub-directories, you can pass an IOFileFilter for the directories.
     * You do not need to AND a DirectoryFileFilter to that filter; this method has already done it for you.
     *
     * Another common use of this method is to find files in a directory tree while ignoring CVS-generated directories.
     *
     * @param directory the directory to search
     * @param fileFilter the filter to apply when looking for files
     * @param dirFilter the optional filter to apply when looking for sub-directories. If null, sub-directories are not searched. TrueFileFilter.INSTANCE matches all directories.
     * @return the matching files as a collection
     * @author K
     * @since 1.0.0
     */
    fun listFiles(directory: File, fileFilter: IOFileFilter, dirFilter: IOFileFilter?): Collection<File> =
        FileUtils.listFiles(directory, fileFilter, dirFilter)

    /**
     * Finds files in the given directory (with optional sub-directories).
     * Returns a collection containing all sub-directories.
     * @see FileKit.listFiles
     *
     * @param directory the directory to search
     * @param fileFilter the filter to use when searching
     * @param dirFilter the optional filter to apply when looking for sub-directories. If null, sub-directories are not searched. TrueFileFilter.INSTANCE matches all directories.
     * @return the matching files as a collection
     * @see org.apache.commons.io.filefilter.NameFileFilter
     * @author K
     * @since 1.0.0
     */
    fun listFilesAndDirs(directory: File, fileFilter: IOFileFilter, dirFilter: IOFileFilter?): Collection<File> =
        FileUtils.listFilesAndDirs(directory, fileFilter, dirFilter)

    /**
     * Allows iteration over the files in the given directory (with optional sub-directories).
     *
     * All found files are filtered through an IOFileFilter. This method builds on
     * [.listFiles] and provides iterable functionality ('foreach' loop).
     *
     * @param directory the directory to search
     * @param fileFilter the filter to use when searching
     * @param dirFilter the optional filter to apply when looking for sub-directories. If null, sub-directories are not searched. TrueFileFilter.INSTANCE matches all directories.
     * @return an iterator over the matching files
     * @see org.apache.commons.io.filefilter.NameFileFilter
     * @author K
     * @since 1.0.0
     */
    fun iterateFiles(directory: File, fileFilter: IOFileFilter, dirFilter: IOFileFilter?): Iterator<File> =
        FileUtils.iterateFiles(directory, fileFilter, dirFilter)

    /**
     * Allows iteration over the files in the given directory (with optional sub-directories).
     *
     * All found files are filtered through an IOFileFilter. This method builds on
     * [.listFiles] and provides iterable functionality ('foreach' loop).
     * The result includes an iterator over sub-directories.
     *
     * @param directory the directory to search
     * @param fileFilter the filter to use when searching
     * @param dirFilter the optional filter to apply when looking for sub-directories. If null, sub-directories are not searched. TrueFileFilter.INSTANCE matches all directories.
     * @return an iterator over the matching files
     * @see org.apache.commons.io.filefilter.NameFileFilter
     * @author K
     * @since 1.0.0
     */
    fun iterateFilesAndDirs(directory: File, fileFilter: IOFileFilter, dirFilter: IOFileFilter?): Iterator<File> =
        FileUtils.iterateFilesAndDirs(directory, fileFilter, dirFilter)

    /**
     * Finds files matching the given extensions in the specified directory (sub-directories are optional).
     *
     * @param directory the directory to search
     * @param extensions an array of extensions, e.g. {"java","xml"}. If null, all files are returned.
     * @param recursive true to search all sub-directories
     * @return list of matching File objects
     * @author K
     * @since 1.0.0
     */
    fun listFiles(directory: File, extensions: Array<String>?, recursive: Boolean): List<File> =
        FileUtils.listFiles(directory, extensions, recursive).toList()

    /**
     * Finds files matching the given extensions in the specified directory (sub-directories are optional).
     * This method builds on [.listFiles] and provides iterable functionality ('foreach' loop).
     *
     * @param directory the directory to search
     * @param extensions an array of extensions, e.g. {"java","xml"}. If null, all files are returned.
     * @param recursive true to search all sub-directories
     * @return an iterator over the matching File objects
     * @author K
     * @since 1.0.0
     */
    fun iterateFiles(directory: File, extensions: Array<String>?, recursive: Boolean): Iterator<File> =
        FileUtils.iterateFiles(directory, extensions, recursive)

    /**
     * Checks whether the contents of two files are equal.
     * This method first checks whether the two files have different lengths, or whether they refer to the same file,
     * and finally compares their contents byte by byte.
     * Throws an IOException if a directory is encountered.
     *
     * @param file1 the first file
     * @param file2 the second file
     * @return true if their contents are equal or neither file exists; otherwise false
     * @author K
     * @since 1.0.0
     */
    fun contentEquals(file1: File, file2: File): Boolean = FileUtils.contentEquals(file1, file2)

    /**
     * Checks whether the contents of two files are equal.
     * This method checks whether the two files refer to the same file,
     * and finally compares their contents line by line.
     * Throws an IOException if a directory is encountered.
     *
     * @param file1 the first file
     * @param file2 the second file
     * @param charsetName the character encoding. If null, the platform default encoding is used.
     * @return true if their contents are equal or neither file exists; otherwise false
     * @author K
     * @since 1.0.0
     */
    fun contentEqualsIgnoreEOL(file1: File, file2: File, charsetName: String?): Boolean =
        FileUtils.contentEqualsIgnoreEOL(file1, file2, charsetName)

    /**
     * Converts a `URL` to a `File`.
     *
     * @param url the url to convert
     * @return the equivalent `File` object; returns `null` if the URL protocol is not `file`
     * @author K
     * @since 1.0.0
     */
    fun toFile(url: URL): File? = FileUtils.toFile(url)

    /**
     * Converts each `URL` to a `File`.
     * Returns an array of the same size as the input array.
     * If the input array contains null elements, the corresponding elements in the output array are also null.
     *
     * The method decodes the URLs. Syntax such as
     * `file:///my%20docs/file.txt` will be correctly decoded to
     * `/my docs/file.txt`.
     *
     * @param urls the array of URLs to convert to File objects
     * @return a non-null array
     *
     * @throws IllegalArgumentException if any URL is not a file
     * @throws IllegalArgumentException if any URL cannot be decoded correctly
     * @author K
     * @since 1.0.0
     */
    fun toFiles(urls: Array<URL>): Array<File?> = FileUtils.toFiles(*urls)

    /**
     * Converts each `File` to a `URL`.
     * Returns an array of the same size as the input array.
     *
     * @param files the array of files to convert
     * @return the converted URL array
     * @throws IOException if a file cannot be converted
     * @author K
     * @since 1.0.0
     */
    fun toURLs(files: Array<File>): Array<URL?> = FileUtils.toURLs(*files)

    /**
     * Copies a file to the specified directory, optionally preserving the file date.
     * This method copies the contents of the source file to a file with the same name in the specified directory.
     * The destination directory will be created if it does not exist. If the destination file exists, it will be overwritten.
     *
     * **Note:** This method attempts to preserve the file's last modified date/time using [File.setLastModified];
     * however, it does not guarantee that the operation will succeed. If the modification operation fails, there is no indication.
     *
     * @param srcFile the existing file to be copied
     * @param destDir the destination directory to place the copied file
     * @param preserveFileDate whether to preserve the original file date
     * @throws IOException if the source or destination is invalid
     * @throws IOException if an I/O error occurs
     * @see .copyFile
     * @author K
     * @since 1.0.0
     */
    fun copyFileToDirectory(srcFile: File, destDir: File, preserveFileDate: Boolean = true): Unit =
        FileUtils.copyFileToDirectory(srcFile, destDir, preserveFileDate)

    /**
     * Copies a file to a new location, optionally preserving the file date.
     * This method copies the contents of the source file to a file with the same name in the specified directory.
     * The destination directory will be created if it does not exist. If the destination file exists, it will be overwritten.
     *
     * **Note:** This method attempts to preserve the file's last modified date/time using [File.setLastModified];
     * however, it does not guarantee that the operation will succeed. If the modification operation fails, there is no indication.
     *
     * @param srcFile the existing file to be copied
     * @param destFile the new file
     * @param preserveFileDate whether to preserve the original file date
     * @throws IOException if the source or destination is invalid
     * @throws IOException if an I/O error occurs during the copy
     * @see .copyFileToDirectory
     * @author K
     * @since 1.0.0
     */
    fun copyFile(srcFile: File, destFile: File, preserveFileDate: Boolean = true): Unit =
        FileUtils.copyFile(srcFile, destFile, preserveFileDate)

    /**
     * Copies bytes from the given file to an `OutputStream`.
     * This method buffers the input internally, so it is not necessary to use a `BufferedInputStream` externally.
     *
     * @param input the file to read data from
     * @param output the `OutputStream` to write to
     * @return the number of bytes copied
     * @throws IOException if an I/O error occurs during the copy
     * @author K
     * @since 1.0.0
     */
    fun copyFile(input: File, output: OutputStream): Long = FileUtils.copyFile(input, output)

    /**
     * Copies a directory into a destination directory, preserving file dates.
     * This method copies the source directory and all its contents into a directory with the same name under the destination.
     *
     * The destination directory will be created if it does not exist.
     * If the destination directory exists, the contents of the source and destination directories are merged, with the source contents overriding the destination contents.
     *
     * **Note:** This method attempts to preserve the file's last modified date/time using [File.setLastModified];
     * however, it does not guarantee that the operation will succeed. If the modification operation fails, there is no indication.
     *
     * @param srcDir an existing directory to be copied
     * @param destDir the destination directory to place the copied source under
     * @throws IOException if the source or destination directory is invalid
     * @throws IOException if an I/O error occurs during the copy
     * @author K
     * @since 1.0.0
     */
    fun copyDirectoryToDirectory(srcDir: File, destDir: File): Unit =
        FileUtils.copyDirectoryToDirectory(srcDir, destDir)

    /**
     * Copies an entire directory to a new location, optionally preserving file dates.
     * This method copies the specified directory and all its sub-directories and files to the destination directory.
     *
     * The destination directory will be created if it does not exist.
     * If the destination directory exists, the contents of the source and destination directories are merged, with the source contents overriding the destination contents.
     *
     * **Note:** This method attempts to preserve the file's last modified date/time using [File.setLastModified];
     * however, it does not guarantee that the operation will succeed. If the modification operation fails, there is no indication.
     *
     * @param srcDir an existing directory to be copied
     * @param destDir the new directory
     * @param preserveFileDate whether to preserve the original file date
     * @throws IOException if the source or destination directory is invalid
     * @throws IOException if an I/O error occurs during the copy
     * @author K
     * @since 1.0.0
     */
    fun copyDirectory(srcDir: File, destDir: File, preserveFileDate: Boolean = true): Unit =
        FileUtils.copyDirectory(srcDir, destDir, preserveFileDate)

    /**
     * Copies a filtered directory to a new location, optionally preserving file dates.
     * This method copies the specified directory and all its sub-directories and files to the destination directory.
     *
     * The destination directory will be created if it does not exist.
     * If the destination directory exists, the contents of the source and destination directories are merged, with the source contents overriding the destination contents.
     *
     * **Note:** This method attempts to preserve the file's last modified date/time using [File.setLastModified];
     * however, it does not guarantee that the operation will succeed. If the modification operation fails, there is no indication.
     *
     * <h4>Example: copy directories only</h4>
     *
     * <pre>
     * // Copy directory structure only
     * FileKit.copyDirectory(srcDir, destDir, DirectoryFileFilter.DIRECTORY);
     * </pre>
     *
     * <h4>Example: copy directories and txt files</h4>
     *
     * <pre>
     *
     * // Copy using a filter
     * FileKit.copyDirectory(srcDir, destDir, filter);
     * </pre>
     *
     * @param srcDir an existing directory to be copied
     * @param destDir the new directory
     * @param filter the filter to use; null means copy all directories and files
     * @param preserveFileDate whether to preserve the file date
     * IOException if the source or destination directory is invalid
     * IOException if an I/O error occurs during the copy
     * @author K
     * @since 1.0.0
     */
    fun copyDirectory(srcDir: File, destDir: File, filter: FileFilter?, preserveFileDate: Boolean = true): Unit =
        FileUtils.copyDirectory(srcDir, destDir, filter, preserveFileDate)

    /**
     * Copies the contents of a URL byte by byte to a file.
     *
     * The destination directory will be created if it does not exist.
     * The destination file will be overwritten if it exists.
     *
     * Warning: This method does not set a connection or read timeout, so it can block forever.
     * To prevent this, use [.copyURLToFile].
     *
     * @param source the `URL` to copy bytes from
     * @param destination the non-directory `File` to write bytes to (may overwrite)
     * @throws IOException if the source URL cannot be opened
     * @throws IOException if the destination is a directory
     * @throws IOException if the destination file cannot be written
     * @throws IOException if the destination needs to be created but cannot be created
     * @throws IOException if an I/O error occurs during the copy
     * @author K
     * @since 1.0.0
     */
    fun copyURLToFile(source: URL, destination: File): Unit = FileUtils.copyURLToFile(source, destination)

    /**
     * Copies the contents of a URL byte by byte to a file.
     * The destination directory will be created if it does not exist.
     * The destination file will be overwritten if it exists.
     *
     * @param source the `URL` to copy bytes from
     * @param destination the non-directory `File` to write bytes to (may overwrite)
     * @param connectionTimeout the timeout in milliseconds if `source` does not establish a connection
     * @param readTimeout the timeout in milliseconds if no data can be read from `source`
     * @throws IOException if the source URL cannot be opened
     * @throws IOException if the destination is a directory
     * @throws IOException if the destination file cannot be written
     * @throws IOException if the destination needs to be created but cannot be created
     * @throws IOException if an I/O error occurs during the copy
     * @author K
     * @since 1.0.0
     */
    fun copyURLToFile(source: URL, destination: File, connectionTimeout: Int, readTimeout: Int): Unit =
        FileUtils.copyURLToFile(source, destination, connectionTimeout, readTimeout)

    /**
     * Copies bytes from an [InputStream] to a file.
     * The destination directory will be created if it does not exist.
     * The destination file will be overwritten if it exists.
     *
     * @param source the `InputStream` to copy data from
     * @param destination the non-directory `File` to write bytes to (may overwrite)
     * @throws IOException if the destination is a directory
     * @throws IOException if the destination file cannot be written
     * @throws IOException if the destination needs to be created but cannot be created
     * @throws IOException if an I/O error occurs during the copy
     * @author K
     * @since 1.0.0
     */
    fun copyInputStreamToFile(source: InputStream, destination: File): Unit =
        FileUtils.copyInputStreamToFile(source, destination)

    /**
     * Recursively deletes a directory.
     *
     * @param directory the directory to delete
     * @throws IOException if the deletion fails
     * @author K
     * @since 1.0.0
     */
    fun deleteDirectory(directory: File): Unit = FileUtils.deleteDirectory(directory)

    /**
     * Deletes a file without throwing an exception. If it is a directory, deletes it and all its sub-directories.
     * The differences between this method and File.delete() are:
     *
     *  * The directory to be deleted does not need to be empty
     *  * No exception is thrown when a directory or file cannot be deleted
     *
     * @param file the file or directory to delete
     * @return `true` if the file or directory was successfully deleted; otherwise `false`
     * @author K
     * @since 1.0.0
     */
    fun deleteQuietly(file: File): Boolean = FileUtils.deleteQuietly(file)

    /**
     * Checks whether a parent directory contains a child directory (or file).
     * Files are not normalized before comparison.
     *
     * Edge cases:
     *  * A directory does not contain itself: returns false
     *
     * @param directory the parent directory
     * @param child the child file or directory
     * @return true if the parent directory contains the child directory or file; otherwise false
     * @throws IOException if an I/O error occurs when checking the file
     * @throws IllegalArgumentException if directory is not a directory
     * @author K
     * @since 1.0.0
     */
    fun directoryContains(directory: File, child: File): Boolean = FileUtils.directoryContains(directory, child)

    /**
     * Empties the given directory rather than deleting it.
     *
     * @param directory the directory to empty
     * IOException if the cleanup is unsuccessful
     * @author K
     * @since 1.0.0
     */
    fun cleanDirectory(directory: File): Unit = FileUtils.cleanDirectory(directory)

    /**
     * Waits for a file to be created, enforcing a timeout.
     *
     * This method repeatedly checks [File.exists] until it returns true within the timeout.
     *
     * @param file the file to check
     * @param seconds the maximum number of seconds to wait
     * @return true if the file exists
     * @author K
     * @since 1.0.0
     */
    fun waitFor(file: File, seconds: Int): Boolean = FileUtils.waitFor(file, seconds)

    /**
     * Reads the contents of a file into a String. The file is always closed.
     *
     * @param file the file to read
     * @param encoding the encoding to use; `null` uses the platform default encoding
     * @return the file contents
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedEncodingException if the specified encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun readFileToString(file: File, encoding: String? = null): String = FileUtils.readFileToString(file, encoding)

    /**
     * Reads the contents of a file into a byte array. The file is always closed.
     *
     * @param file the file to read
     * @return the byte array of the file contents
     * @throws IOException if an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun readFileToByteArray(file: File): ByteArray = FileUtils.readFileToByteArray(file)

    /**
     * Reads the contents of a file line by line into a list of Strings. The file is always closed.
     *
     * @param file the file to read
     * @param encoding the encoding to use; `null` uses the platform default encoding
     * @return a list of Strings, each element representing a line in the file
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedEncodingException if the encoding is not supported
     * @author K
     * @since 1.0.0
     */
    fun readLines(file: File, encoding: String? = null): List<String> = FileUtils.readLines(file, encoding)

    /**
     * Returns an iterator over the lines in a file.
     * This method opens an `InputStream` for the given file. After iterating, you must
     * close the stream to free internal resources. This can be done by calling [LineIterator.close] or
     * [LineIterator.closeQuietly].
     *
     * Example usage:
     *
     * <pre>
     * LineIterator it = FileKit.lineIterator(file, &quot;UTF-8&quot;);
     * try {
     * while (it.hasNext()) {
     * String line = it.nextLine();
     * // / do something with line
     * }
     * } finally {
     * LineIterator.closeQuietly(iterator);
     * }
     * </pre>
     *
     * If an exception occurs while creating the iterator, the underlying stream is closed.
     *
     * @param file the file object to read
     * @param encoding the encoding to use; `null` uses the platform default encoding
     * @return an iterator over the lines of the file
     * @throws IOException if an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun lineIterator(file: File, encoding: String? = null): LineIterator = FileUtils.lineIterator(file, encoding)

    /**
     * Writes a String to a file, creating the file if it does not exist.
     *
     * @param file the file to write to
     * @param data the contents to write to the file
     * @param encoding the encoding to use; `null` uses the platform default encoding
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedEncodingException if the specified encoding is not supported by the VM
     * @author K
     * @since 1.0.0
     */
    fun writeStringToFile(file: File, data: String, encoding: String? = null): Unit =
        FileUtils.writeStringToFile(file, data, encoding)

    /**
     * Writes a String to a file, creating the file if it does not exist.
     *
     * @param file the file to write to
     * @param data the contents to write to the file
     * @param encoding the encoding to use; `null` uses the platform default encoding
     * @param append whether the character sequence is appended to the end of the file rather than overwriting it
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedEncodingException if the specified encoding is not supported by the VM
     * @author K
     * @since 1.0.0
     */
    fun writeStringToFile(file: File, data: String, encoding: String? = null, append: Boolean): Unit =
        FileUtils.writeStringToFile(file, data, encoding, append)

    /**
     * Writes a character sequence to a file, creating the file if it does not exist.
     *
     * @param file the file to write to
     * @param data the contents to write to the file
     * @param encoding the encoding to use; `null` uses the platform default encoding
     * @param append whether the character sequence is appended to the end of the file rather than overwriting it
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedEncodingException if the specified encoding is not supported by the VM
     * @author K
     * @since 1.0.0
     */
    fun write(file: File, data: CharSequence, encoding: String? = null, append: Boolean = false): Unit =
        FileUtils.write(file, data, encoding, append)

    /**
     * Writes a byte array to a file using the VM default encoding, creating the file if it does not exist.
     *
     * @param file the file to write to
     * @param data the contents to write to the file
     * @param append whether the data is appended to the end of the file rather than replacing the file contents
     * @throws IOException if an I/O error occurs
     * @author K
     * @since 1.0.0
     */
    fun writeByteArrayToFile(file: File, data: ByteArray, append: Boolean = false): Unit =
        FileUtils.writeByteArrayToFile(file, data, append)

    /**
     * Writes the toString() value of each element in a collection line by line to the given file.
     * Uses the specified encoding and line separator.
     * The file is created if it does not exist.
     *
     * @param file the file to write to
     * @param encoding the encoding to use; `null` uses the platform default encoding
     * @param lines the collection to write; `null` writes blank lines
     * @param lineEnding the line separator; `null` uses the system default line separator
     * @param append whether the data is appended to the end of the file rather than replacing the file contents
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedEncodingException if the specified encoding is not supported by the VM
     * @author K
     * @since 1.0.0
     */
    fun writeLines(
        file: File,
        encoding: String? = null,
        lines: Collection<*>? = null,
        lineEnding: String? = null,
        append: Boolean = false
    ): Unit =
        FileUtils.writeLines(file, encoding, lines, lineEnding, append)

    /**
     * Deletes a file. If it is a directory, deletes it and all its sub-directories.
     * The differences between this method and File.delete() are:
     *   The directory to be deleted can be non-empty
     *   An exception is thrown when a directory or file cannot be deleted (File's method returns a boolean)
     *
     * @param file the file or directory to delete
     * @throws FileNotFoundException if the directory or file cannot be found
     * @throws IOException if the deletion fails
     * @author K
     * @since 1.0.0
     */
    fun forceDelete(file: File): Unit = FileUtils.forceDelete(file)

    /**
     * Deletes the given file or directory when the Java virtual machine exits. If it is a directory, deletes the directory and all its sub-directories.
     *
     * @param file the file or directory to delete
     * @throws IOException if the deletion fails
     * @author K
     * @since 1.0.0
     */
    fun forceDeleteOnExit(file: File): Unit = FileUtils.forceDeleteOnExit(file)

    /**
     * Creates the directory, including any necessary but non-existent parent directories.
     * If a file with the given name already exists but is not a directory, an IOException is thrown.
     * If the directory cannot be created (or exists but is not a directory), an IOException is thrown.
     *
     * @param directory the directory to create
     * @throws IOException if the directory cannot be created or exists but is not a directory
     * @author K
     * @since 1.0.0
     */
    fun forceMkdir(directory: File): Unit = FileUtils.forceMkdir(directory)

    /**
     * Returns the size of the given file or directory.
     * If the provided [File] is a regular file, the length of the file is returned.
     * If the argument is a directory, the size of the directory is computed recursively. If a directory or sub-directory
     * has security restrictions, its size is not included.
     *
     * @param file the regular file or directory whose size is to be returned
     * @return the length of the file, or the recursive size of the directory (in bytes)
     * @throws IllegalArgumentException if the file does not exist
     * @author K
     * @since 1.0.0
     */
    fun sizeOf(file: File): Long = FileUtils.sizeOf(file)

    /**
     * Returns the size of the given file or directory.
     * If the provided [File] is a regular file, the length of the file is returned.
     * If the argument is a directory, the size of the directory is computed recursively. If a directory or sub-directory
     * has security restrictions, its size is not included.
     *
     * @param file the regular file or directory whose size is to be returned
     * @return the length of the file, or the recursive size of the directory (in bytes)
     * @throws IllegalArgumentException if the file does not exist
     * @author K
     * @since 1.0.0
     */
    fun sizeOfAsBigInteger(file: File): BigInteger = FileUtils.sizeOfAsBigInteger(file)

    /**
     * Recursively computes the size of a directory (the sum of the sizes of all files).
     *
     * @param directory the directory to check
     * @return the size of the directory (in bytes); returns 0 if the directory has security restrictions; returns a negative number when the total size is greater than [Long.MAX_VALUE]
     * @author K
     * @since 1.0.0
     */
    fun sizeOfDirectory(directory: File): Long = FileUtils.sizeOfDirectory(directory)

    /**
     * Recursively computes the size of a directory (the sum of the sizes of all files).
     *
     * @param directory the directory to check
     * @return the size of the directory (in bytes); returns 0 if the directory has security restrictions
     * @author K
     * @since 1.0.0
     */
    fun sizeOfDirectoryAsBigInteger(directory: File): BigInteger = FileUtils.sizeOfDirectoryAsBigInteger(directory)

    /**
     * Checks whether the given first file is newer (by modification date) than the second file.
     *
     * @param file the first file
     * @param reference the second file
     * @return true if the file exists and is newer than the second file
     * @throws IllegalArgumentException if the file does not exist
     * @author K
     * @since 1.0.0
     */
    fun isFileNewer(file: File, reference: File): Boolean = FileUtils.isFileNewer(file, reference)

    /**
     * Checks whether the modification date of the given file is newer than the specified date.
     *
     * @param file the file to compare
     * @param timeMillis the date as milliseconds
     * @return true if the file exists and was modified after the given date
     * @throws IllegalArgumentException if the file is `null`
     * @author K
     * @since 1.0.0
     */
    fun isFileNewer(file: File, timeMillis: Long): Boolean = FileUtils.isFileNewer(file, timeMillis)

    /**
     * Checks whether the given first file is older (by modification date) than the second file.
     *
     * @param file the first file
     * @param reference the second file
     * @return true if the file exists and is older than the second file
     * @throws    IllegalArgumentException if the file does not exist
     * @author K
     * @since 1.0.0
     */
    fun isFileOlder(file: File, reference: File): Boolean = FileUtils.isFileOlder(file, reference)

    /**
     * Checks whether the modification date of the given file is older than the specified date.
     *
     * @param file the file to compare
     * @param timeMillis the date as milliseconds
     * @return true if the file exists and was modified before the given date
     * @author K
     * @since 1.0.0
     */
    fun isFileOlder(file: File, timeMillis: Long): Boolean = FileUtils.isFileOlder(file, timeMillis)

    /**
     * Computes the checksum of a file using the CRC32 checksum algorithm. Returns the checksum.
     *
     * @param file the file to compute the checksum of
     * @return the checksum
     * @throws IllegalArgumentException if the given file is a directory
     * @throws IOException if an I/O error occurs while reading the file
     * @author K
     * @since 1.0.0
     */
    fun checksumCRC32(file: File): Long = FileUtils.checksumCRC32(file)

    /**
     * Computes the checksum of a file using the given checksum object.
     * Multiple files can be checksummed with the same `Checksum` object. For example:
     *
     * <pre>
     * long csum = FileKit.checksum(file, new CRC32()).getValue();
     * </pre>
     *
     * @param file the file to compute the checksum of
     * @param checksum the checksum object to use
     * @return the given checksum object, updated with the contents of the file
     * @throws IllegalArgumentException if the given file is a directory
     * @throws IOException if an I/O error occurs while reading the file
     * @author K
     * @since 1.0.0
     */
    fun checksum(file: File, checksum: Checksum): Checksum = FileUtils.checksum(file, checksum)

    /**
     * Moves a directory to another directory.
     * When the destination is on another file system, performs a copy and delete.
     *
     * @param srcDir the directory to move
     * @param destDir the destination directory
     * @throws FileNotFoundException if the destination directory exists
     * @throws IOException if the source or destination is unavailable
     * @throws IOException if an I/O error occurs during the move
     * @author K
     * @since 1.0.0
     */
    fun moveDirectory(srcDir: File, destDir: File): Unit = FileUtils.moveDirectory(srcDir, destDir)

    /**
     * Moves a directory to another directory.
     * When the destination is on another file system, performs a copy and delete.
     *
     * @param srcDir the directory to move
     * @param destDir the destination directory
     * @param createDestDir `true` to create the directory
     * @throws FileNotFoundException if the destination directory exists
     * @throws IOException if the source or destination is unavailable
     * @throws IOException if an I/O error occurs during the move
     * @author K
     * @since 1.0.0
     */
    fun moveDirectoryToDirectory(srcDir: File, destDir: File, createDestDir: Boolean): Unit =
        FileUtils.moveDirectoryToDirectory(srcDir, destDir, createDestDir)

    /**
     * Moves a file.
     * When the destination is on another file system, performs a copy and delete.
     *
     * @param srcFile the file to move
     * @param destFile the destination file
     * @throws FileNotFoundException if the destination file exists
     * @throws IOException if the source or destination is unavailable
     * @throws IOException if an I/O error occurs during the move
     * @author K
     * @since 1.0.0
     */
    fun moveFile(srcFile: File, destFile: File): Unit = FileUtils.moveFile(srcFile, destFile)

    /**
     * Moves a file.
     * When the destination is on another file system, performs a copy and delete.
     *
     * @param srcFile the file to move
     * @param destDir the destination directory
     * @param createDestDir `true` to create the directory
     * @throws FileNotFoundException if the destination file exists
     * @throws IOException if the source or destination is unavailable
     * @throws IOException if an I/O error occurs during the move
     * @author K
     * @since 1.0.0
     */
    fun moveFileToDirectory(srcFile: File, destDir: File, createDestDir: Boolean): Unit =
        FileUtils.moveFileToDirectory(srcFile, destDir, createDestDir)

    /**
     * Moves a file or directory to a destination directory.
     * When the destination is on another file system, performs a copy and delete.
     *
     * @param src the file or directory to move
     * @param destDir the destination directory
     * @throws FileNotFoundException if the file or directory already exists in the destination directory
     * @throws IOException if the source or destination is unavailable
     * @throws IOException if an I/O error occurs during the move
     * @author K
     * @since 1.0.0
     */
    fun moveToDirectory(src: File, destDir: File, createDestDir: Boolean): Unit =
        FileUtils.moveToDirectory(src, destDir, createDestDir)

    /**
     * Determines whether the given file is a symbolic link rather than an actual file.
     * Returns false if any part of the path is a symbolic link. Returns true only if the given file itself is an actual file.
     *
     * **Note:** If the current system is detected as Windows,
     * the current implementation always returns `false`.
     *
     * @param file the file to check
     * @return true if the file is a symbolic link
     * @throws IOException if an I/O error occurs while checking
     * @author K
     * @since 1.0.0
     */
    fun isSymlink(file: File): Boolean = FileUtils.isSymlink(file)

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // Wraps org.apache.commons.io.FileUtils
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

}
