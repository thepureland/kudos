package io.kudos.tools.codegen.model.vo

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * 生成的文件信息值对象
 *
 * @author K
 * @since 1.0.0
 */
class GenFile(
    generate: Boolean,

    filename: String,

    directory: String,

    /** 参数化（Freemarker 处理 `${entityName}` 等占位符后）的文件相对路径 */
    var finalFileRelativePath: String,

    /** 模板文件相对路径，生成时据此从模板根目录读取对应模板 */
    var templateFileRelativePath: String
) : Comparable<GenFile> {

    /** 是否生成此文件，绑定到 UI 的复选框 */
    private val generate = SimpleBooleanProperty()
    /** 目标文件名（已替换占位符） */
    private val filename = SimpleStringProperty()
    /** 目标文件所在目录的绝对路径（已替换占位符） */
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
     * 按目录路径排序，让生成列表在 UI 上按目录归组展示。
     */
    override fun compareTo(other: GenFile): Int = getDirectory().compareTo(other.getDirectory())
}