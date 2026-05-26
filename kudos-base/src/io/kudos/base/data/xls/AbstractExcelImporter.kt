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
 * Abstract base class for Excel data importers.
 * Notes:
 * 1. The first row of the Excel file is the column header (description), not data; it is skipped during import.
 * 2. Row-object classes may be data classes or plain classes.
 *    For data classes all properties must be declared in the primary constructor and may be read-only (val);
 *    for plain classes a no-argument constructor is required and properties must be read-write (var).
 * 3. The default validation implementation uses Kudos's bean validation (ValidationKit).
 * 4. All error messages are reported through IllegalStateException.
 * 5. To customize cell-value handling, override getPropertyValue.
 * 6. To implement more sophisticated validation, override validate.
 *
 * @author K
 * @since 1.0.0
 */
abstract class AbstractExcelImporter<T : Any> : IExcelImporter<T> {

    /** Logger. */
    private val log = LogFactory.getLog(AbstractExcelImporter::class)

    /**
     * Input stream of the uploaded Excel file.
     */
    private lateinit var inputStream: InputStream

    /**
     * The first sheet.
     */
    private lateinit var sheet: Sheet

    /** Property reflection cache: caches [KProperty1] by property name to avoid re-reflecting on each row. */
    private val propertyMap = mutableMapOf<String, KProperty1<T, Any?>>()

    /** Property names ordered to match the Excel columns (provided by [getPropertyNames]). */
    private lateinit var propertyNames: List<String>

    /**
     * Returns the property names in Excel column order.
     *
     * @return list of property names
     * @author K
     * @since 1.0.0
     */
    protected abstract fun getPropertyNames(): List<String>

    /**
     * Returns the name of the sheet that contains the data.
     *
     * @return sheet name
     * @author K
     * @since 1.0.0
     */
    protected abstract fun getSheetName(): String

    /**
     * Saves the data.
     *
     * @param rowObjects list of row objects
     * @author K
     * @since 1.0.0
     */
    protected abstract fun save(rowObjects: List<T>)

    /**
     * Validates the data.
     *
     * @param rowObjects list of row objects
     * @author K
     * @since 1.0.0
     */
    protected open fun validate(rowObjects: List<T>) {
        for (rowObject in rowObjects) {
            val violations = ValidationKit.validateBean(rowObject)
            if (violations.isNotEmpty()) {
                error("Imported data failed validation: ${violations.first().message}")
            }
        }
    }

    /**
     * Imports an Excel file.
     *
     * Executes the full import flow: check the template, parse data, validate, and save.
     *
     * Workflow:
     * 1. Open the file input stream via FileKit.openInputStream.
     * 2. Check the template: verify the Excel format and that the sheet exists.
     * 3. Wrap row objects: convert each Excel row to a Java object.
     * 4. Validate: run validation against the converted objects.
     * 5. Save: invoke the subclass's save implementation.
     * 6. Return: return the list of imported row objects.
     *
     * Resource management:
     * - Uses the `use` extension to ensure the input stream is closed correctly.
     * - Resources are released even if an exception is thrown.
     *
     * Exception handling:
     * - Template check failures throw IllegalStateException.
     * - Data-parse failures throw IllegalStateException.
     * - Validation failures throw IllegalStateException.
     *
     * Notes:
     * - The first Excel row is skipped (treated as the header).
     * - Data is parsed starting from the second row.
     * - All errors are propagated via exceptions -- never silently swallowed.
     *
     * @param xlsFile Excel file
     * @return list of imported row objects
     * @throws IllegalStateException when any error occurs during import
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
     * Checks whether the Excel template is correct.
     *
     * Verifies the Excel file format and that the configured sheet exists.
     *
     * Workflow:
     * 1. Open the workbook with Workbook.getWorkbook.
     * 2. Get the sheet name from the subclass via getSheetName.
     * 3. Look up the sheet in the workbook.
     * 4. If the sheet is missing, throw an exception.
     *
     * What is verified:
     * - The Excel file format is correct.
     * - The configured sheet exists.
     *
     * Exception handling:
     * - File-format errors raise the corresponding exception.
     * - Missing sheets raise IllegalStateException.
     *
     * Notes:
     * - Uses the `use` extension to ensure the workbook is released properly.
     * - Sheet name matching is case-sensitive and must match exactly.
     *
     * @throws IllegalStateException when the template is invalid or the sheet does not exist
     */
    protected open fun checkTemplate() {
        inputStream.use {
            val workbook = Workbook.getWorkbook(it)
            val sheetName = getSheetName()
            sheet = workbook.getSheet(sheetName) ?: error("Sheet named [$sheetName] not found!")
        }
    }

    /**
     * Wraps each Excel row into an object.
     *
     * Walks every row of the Excel sheet (skipping the header) and converts cell data into Java objects.
     *
     * Workflow:
     * 1. Get the property names from the subclass via getPropertyNames.
     * 2. Resolve the row-object type via reflection on the generic parameter.
     * 3. Walk data rows starting at row index 1 (row 0 is the header).
     * 4. For each row:
     *    - Read all cells in the row.
     *    - Map the column index to a property name.
     *    - Call getPropertyValue to obtain the value.
     *    - Assign the value based on whether the type is a data class or plain class.
     * 5. Append the constructed object to the result list.
     *
     * Object construction:
     * - Data class: collect all property values first, then invoke the primary constructor.
     * - Plain class: create an empty instance, then set each property.
     *
     * Property mapping:
     * - Column index maps directly to the property-name list index.
     * - Column 1 -> property 1, column 2 -> property 2, and so on.
     *
     * Exception handling:
     * - Read errors are logged and re-thrown as IllegalStateException.
     * - Type-conversion failures are handled inside getPropertyValue.
     *
     * Notes:
     * - Row 0 is skipped as the header.
     * - The property-name list size must equal the Excel column count.
     * - Data classes must declare all properties in the primary constructor.
     * - Plain classes must provide a no-argument constructor and `var` properties.
     *
     * @return list of imported row objects
     * @throws IllegalStateException when reading data fails
     */
    protected open fun wrapRowObjects(): List<T> {
        val rowObjectList = mutableListOf<T>()
        propertyNames = getPropertyNames()
        try {
            val rows = sheet.rows
            val rowObjectClass = resolveRowObjectClass()
            lateinit var rowObject: T
            for (row in 1 until rows) { // Skip the header.
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
                            ?: error("Property [$propertyName] must be writable (var)")
                        mutableProp.setter.call(rowObject, value)
                    }
                }
                if (rowObjectClass.isData) {
                    val constructor = requireNotNull(rowObjectClass.primaryConstructor) {
                        "Data class must have a primary constructor: ${rowObjectClass.qualifiedName}"
                    }
                    val values = constructor.parameters.map { parameter ->
                        val paramName = requireNotNull(parameter.name) {
                            "Primary constructor parameter name is null: ${rowObjectClass.qualifiedName}"
                        }
                        requireNotNull(propNameValueMap[paramName]) {
                            "Missing value for data class constructor parameter: ${rowObjectClass.qualifiedName}.$paramName"
                        }
                    }
                    rowObject = rowObjectClass.newInstance(*values.toTypedArray())
                }
                rowObjectList.add(rowObject)
            }
        } catch (ex: IllegalArgumentException) {
            log.error(ex)
            error("Error reading Excel data!")
        } catch (ex: IllegalStateException) {
            log.error(ex)
            error("Error reading Excel data!")
        } catch (ex: ClassCastException) {
            log.error(ex)
            error("Error reading Excel data!")
        } catch (ex: IndexOutOfBoundsException) {
            log.error(ex)
            error("Error reading Excel data!")
        }
        return rowObjectList
    }

    /**
     * Returns the property value.
     *
     * Extracts the value from an Excel cell and converts it to the property's type.
     *
     * Workflow:
     * 1. Look up the property name by column index.
     * 2. Get the property object from the cache or via reflection.
     * 3. Read the cell's string content.
     * 4. If the cell is numeric, convert the value to the property's actual type.
     * 5. Return the converted value.
     *
     * Property cache:
     * - propertyMap caches property objects to avoid repeated reflection.
     * - Reduces reflection overhead and improves performance.
     *
     * Type conversion:
     * - For NUMBER cells, the value is converted to match the property's runtime type.
     * - Conversion is performed via the toType extension.
     * - Supports primitive and boxed types.
     *
     * Return value:
     * - Numeric cells: converted to the appropriate numeric type (Int, Long, Double, ...).
     * - Other cells: the string content is returned.
     *
     * Extension point:
     * - Override this method to add custom conversion logic, such as date formatting or enum conversion.
     *
     * Notes:
     * - The property-name list must match the Excel column order.
     * - Numeric conversion failures throw an exception.
     * - Non-numeric cells return the string content directly.
     *
     * @param rowObjectClass row-object class
     * @param columnIndex column index (0-based)
     * @param cell Excel cell object
     * @return the converted property value
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

    /**
     * Resolves the actual row-object class from the subclass's generic argument [T] via reflection.
     * Throws immediately when the subclass erased the type argument (resolved to [Nothing]) to avoid
     * silent failures during reflective construction.
     *
     * @return [KClass] of the row object
     * @throws IllegalArgumentException when the generic argument cannot be resolved
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    private fun resolveRowObjectClass(): KClass<T> {
        val rowObjectClass = GenericKit.getSuperClassGenricClass(this::class)
        require(rowObjectClass != Nothing::class) { "Cannot resolve imported row-object type: ${this::class.qualifiedName}" }
        return rowObjectClass as KClass<T>
    }

}