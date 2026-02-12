package io.kudos.base.data.xls

import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.io.FileKit
import io.kudos.base.lang.GenericKit
import io.kudos.base.lang.reflect.getMemberProperty
import io.kudos.base.lang.reflect.newInstance
import io.kudos.base.lang.string.toType
import io.kudos.base.logger.LogFactory
import jxl.Cell
import jxl.CellType
import jxl.Sheet
import jxl.Workbook
import java.io.File
import java.io.InputStream
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.primaryConstructor

/**
 * excel数据导入器抽象类
 * 注意事项：
 * 1. Excel第一行为列描述信息，并非要导入的数据，在导入时第一行会被忽略
 * 2. 行对象类支持数据类和普通类。
 *    为数据类时，属性必须全部定义在主构造函数中，可以是只读的(val);为普通类时，必须存在空构造函数，属性只能是可读可写的(var)。
 * 3. 数据校验的默认实现是Kudos的bean校验方式(ValidationKit)
 * 4. 错误消息全部通过IllegalStateException异常抛出
 * 5. 如果需要对单元格的值作特殊处理，可重写getPropertyValue方法
 * 6. 如果需要复杂的校验逻辑，可重写validate方法
 *
 * @author K
 * @since 1.0.0
 */
abstract class AbstractExcelImporter<T : Any> : IExcelImporter<T> {

    private val log = LogFactory.getLog(AbstractExcelImporter::class)

    /**
     * 上传的excel文件的输入流
     */
    private lateinit var inputStream: InputStream

    /**
     * 第一个sheet页
     */
    private lateinit var sheet: Sheet

    private val propertyMap = mutableMapOf<String, KProperty1<T, Any?>>()

    private lateinit var propertyNames: List<String>

    /**
     * 按excel中的列顺序返回对应的属性名列表
     *
     * @return List(属性名)
     * @author K
     * @since 1.0.0
     */
    protected abstract fun getPropertyNames(): List<String>

    /**
     * 返回数据所在sheet页的名称
     *
     * @return sheet页的名称
     * @author K
     * @since 1.0.0
     */
    protected abstract fun getSheetName(): String

    /**
     * 保存数据
     *
     * @param rowObjects 行对象列表
     * @author K
     * @since 1.0.0
     */
    protected abstract fun save(rowObjects: List<T>)

    /**
     * 检查数据合法性
     *
     * @param rowObjects 行对象列表
     * @author K
     * @since 1.0.0
     */
    protected open fun validate(rowObjects: List<T>) {
        for (rowObject in rowObjects) {
            val violations = ValidationKit.validateBean(rowObject)
            if (violations.isNotEmpty()) {
                error("导入的数据校验不通过：${violations.first().message}")
            }
        }
    }

    /**
     * 导入Excel文件
     * 
     * 执行完整的Excel导入流程：检查模板、解析数据、验证数据、保存数据。
     * 
     * 工作流程：
     * 1. 打开文件输入流：使用FileKit.openInputStream打开文件
     * 2. 检查模板：验证Excel文件格式和Sheet页是否存在
     * 3. 包装行对象：将Excel的每一行数据转换为Java对象
     * 4. 验证数据：对转换后的对象进行数据校验
     * 5. 保存数据：调用子类实现的save方法保存数据
     * 6. 返回结果：返回导入的行对象列表
     * 
     * 资源管理：
     * - 使用use扩展函数确保输入流会被正确关闭
     * - 即使发生异常，资源也会被正确释放
     * 
     * 异常处理：
     * - 如果模板检查失败，会抛出IllegalStateException
     * - 如果数据解析失败，会抛出IllegalStateException
     * - 如果数据验证失败，会抛出IllegalStateException
     * 
     * 注意事项：
     * - Excel第一行会被忽略（作为列头）
     * - 数据从第二行开始解析
     * - 所有错误都会通过异常抛出，不会静默失败
     * 
     * @param xlsFile Excel文件对象
     * @return 导入的行对象列表
     * @throws IllegalStateException 如果导入过程中发生任何错误
     */
    override fun import(xlsFile: File): List<T> {
        return FileKit.openInputStream(xlsFile).use {
            this.inputStream = it
            checkTemplate()
            val rowObjects = wrapRowObjects()
            validate(rowObjects)
            save(rowObjects)
            rowObjects
        }
    }

    /**
     * 检查Excel模板是否正确
     * 
     * 验证Excel文件格式和指定的Sheet页是否存在。
     * 
     * 工作流程：
     * 1. 打开工作簿：使用Workbook.getWorkbook读取Excel文件
     * 2. 获取Sheet名称：调用子类实现的getSheetName方法
     * 3. 查找Sheet页：在工作簿中查找指定名称的Sheet页
     * 4. 验证结果：如果找不到Sheet页，抛出异常
     * 
     * 验证内容：
     * - Excel文件格式是否正确
     * - 指定的Sheet页是否存在
     * 
     * 异常处理：
     * - 如果文件格式错误，会抛出相应的异常
     * - 如果Sheet页不存在，会抛出IllegalStateException
     * 
     * 注意事项：
     * - 使用use扩展函数确保工作簿资源被正确释放
     * - Sheet页名称必须完全匹配（区分大小写）
     * 
     * @throws IllegalStateException 如果模板不正确或Sheet页不存在
     */
    protected open fun checkTemplate() {
        inputStream.use {
            val workbook = Workbook.getWorkbook(it)
            val sheetName = getSheetName()
            sheet = workbook.getSheet(sheetName) ?: error("找不到名称【$sheetName】对应的Sheet页！")
        }
    }

    /**
     * 将Excel的每行数据包装成对象
     * 
     * 遍历Excel的每一行（跳过第一行列头），将单元格数据转换为Java对象。
     * 
     * 工作流程：
     * 1. 获取属性名列表：调用子类实现的getPropertyNames方法
     * 2. 获取行对象类型：通过反射获取泛型参数的实际类型
     * 3. 遍历数据行：从第二行开始（第一行是列头）
     * 4. 处理每行数据：
     *    - 获取行中的所有单元格
     *    - 根据列索引获取对应的属性名
     *    - 调用getPropertyValue获取属性值
     *    - 根据对象类型（数据类或普通类）设置属性值
     * 5. 添加到列表：将创建的对象添加到结果列表
     * 
     * 对象创建方式：
     * - 数据类：先收集所有属性值，然后通过主构造函数创建对象
     * - 普通类：先创建空对象，然后逐个设置属性值
     * 
     * 属性映射：
     * - 列索引对应属性名列表的索引
     * - 第一列对应第一个属性，第二列对应第二个属性，以此类推
     * 
     * 异常处理：
     * - 如果读取数据出错，会记录错误日志并抛出IllegalStateException
     * - 如果属性值类型转换失败，会在getPropertyValue中处理
     * 
     * 注意事项：
     * - 第一行（索引0）会被跳过，作为列头
     * - 属性名列表的大小必须与Excel列数匹配
     * - 数据类要求所有属性都在主构造函数中
     * - 普通类要求有空构造函数且属性为var
     * 
     * @return 导入的行对象列表
     * @throws IllegalStateException 如果读取数据出错
     */
    protected open fun wrapRowObjects(): List<T> {
        val rowObjectList = mutableListOf<T>()
        propertyNames = getPropertyNames()
        try {
            val rows = sheet.rows
            val rowObjectClass = resolveRowObjectClass()
            lateinit var rowObject: T
            for (row in 1 until rows) { // 扣掉列头
                val rowCells = sheet.getRow(row)
                val propNameValueMap = mutableMapOf<String, Any>()
                if (!rowObjectClass.isData) {
                    rowObject = rowObjectClass.newInstance()
                }
                for (columnIndex in rowCells.indices) {
                    val cell = rowCells[columnIndex]
                    val value = getPropertyValue(rowObjectClass, columnIndex, cell)
                    val propertyName = propertyNames[columnIndex]
                    if (rowObjectClass.isData) {
                        propNameValueMap[propertyName] = value
                    } else {
                        val mutableProp = propertyMap[propertyName] as? KMutableProperty1<*, *>
                            ?: error("属性【$propertyName】必须是可写属性(var)")
                        mutableProp.setter.call(rowObject, value)
                    }
                }
                if (rowObjectClass.isData) {
                    val constructor = requireNotNull(rowObjectClass.primaryConstructor) {
                        "数据类必须存在主构造函数: ${rowObjectClass.qualifiedName}"
                    }
                    val values = constructor.parameters.map { parameter ->
                        val paramName = requireNotNull(parameter.name) {
                            "主构造函数参数名为空: ${rowObjectClass.qualifiedName}"
                        }
                        requireNotNull(propNameValueMap[paramName]) {
                            "缺少数据类构造参数值: ${rowObjectClass.qualifiedName}.$paramName"
                        }
                    }
                    rowObject = rowObjectClass.newInstance(*values.toTypedArray())
                }
                rowObjectList.add(rowObject)
            }
        } catch (ex: IllegalArgumentException) {
            log.error(ex)
            error("读取excel数据出错!")
        } catch (ex: IllegalStateException) {
            log.error(ex)
            error("读取excel数据出错!")
        } catch (ex: ClassCastException) {
            log.error(ex)
            error("读取excel数据出错!")
        } catch (ex: IndexOutOfBoundsException) {
            log.error(ex)
            error("读取excel数据出错!")
        }
        return rowObjectList
    }

    /**
     * 获取属性值
     * 
     * 从Excel单元格中提取值，并根据属性类型进行类型转换。
     * 
     * 工作流程：
     * 1. 获取属性名：根据列索引从属性名列表中获取对应的属性名
     * 2. 获取属性对象：从缓存中获取或通过反射获取属性对象
     * 3. 提取单元格内容：获取单元格的字符串内容
     * 4. 类型转换：如果单元格类型是数字，根据属性类型进行转换
     * 5. 返回结果：返回转换后的属性值
     * 
     * 属性缓存：
     * - 使用propertyMap缓存已获取的属性对象，避免重复反射
     * - 提高性能，减少反射开销
     * 
     * 类型转换：
     * - 如果单元格类型是NUMBER，会根据属性的实际类型进行转换
     * - 使用toType扩展函数进行类型转换
     * - 支持基本类型和包装类型的转换
     * 
     * 返回值：
     * - 数字类型：转换为对应的数值类型（Int、Long、Double等）
     * - 其他类型：返回字符串内容
     * 
     * 扩展点：
     * - 子类可以重写此方法，实现自定义的类型转换逻辑
     * - 例如：日期格式转换、枚举值转换等
     * 
     * 注意事项：
     * - 属性名列表必须与Excel列顺序一致
     * - 数字类型转换失败会抛出异常
     * - 非数字类型的单元格直接返回字符串内容
     * 
     * @param rowObjectClass 行对象类
     * @param columnIndex 列索引（从0开始）
     * @param cell Excel单元格对象
     * @return 转换后的属性值
     */
    protected open fun getPropertyValue(rowObjectClass: KClass<T>, columnIndex: Int, cell: Cell): Any {
        val propertyName = propertyNames[columnIndex]
        var prop = propertyMap[propertyName]
        if (prop == null) {
            prop = rowObjectClass.getMemberProperty(propertyName)
            propertyMap[propertyName] = prop
        }
        val valueStr = cell.contents
        var value: Any = valueStr
        val type = cell.type
        if (type === CellType.NUMBER) {
            val argType = prop.returnType.classifier as KClass<*>
            value = valueStr.toType(argType)
        }
        return value
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveRowObjectClass(): KClass<T> {
        val rowObjectClass = GenericKit.getSuperClassGenricClass(this::class)
        require(rowObjectClass != Nothing::class) { "无法解析导入行对象类型: ${this::class.qualifiedName}" }
        return rowObjectClass as KClass<T>
    }

}