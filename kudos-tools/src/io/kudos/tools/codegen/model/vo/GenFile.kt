package io.kudos.tools.codegen.model.vo

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * Value object for generated file information.
 *
 * @author K
 * @since 1.0.0
 */
class GenFile(
    generate: Boolean,

    filename: String,

    directory: String,

    /** Parameterized (after Freemarker processes placeholders such as `${entityName}`) relative file path */
    var finalFileRelativePath: String,

    /** Template file relative path; used to read the corresponding template from the template root at generation time */
    var templateFileRelativePath: String
) : Comparable<GenFile> {

    /** Whether to generate this file; bound to a UI checkbox */
    private val generate = SimpleBooleanProperty()
    /** Target file name (placeholders already replaced) */
    private val filename = SimpleStringProperty()
    /** Absolute path of the target file's directory (placeholders already replaced) */
    private val directory = SimpleStringProperty()

    init {
        setGenerate(generate)
        setFilename(filename)
        setDirectory(directory)
    }

    fun getGenerate(): Boolean = generate.get()

    fun generateProperty(): BooleanProperty = generate

    fun setGenerate(generate: Boolean) = this.generate.set(generate)

    fun getFilename(): String = filename.get()

    fun setFilename(filename: String) = this.filename.set(filename)

    fun filenameProperty(): StringProperty = filename

    fun getDirectory(): String = directory.get()

    fun setDirectory(directory: String) = this.directory.set(directory)

    fun directoryProperty(): StringProperty = directory

    /**
     * Sort by directory path so the generation list groups by directory in the UI.
     */
    override fun compareTo(other: GenFile): Int = getDirectory().compareTo(other.getDirectory())
}