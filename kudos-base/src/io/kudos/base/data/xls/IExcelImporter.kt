package io.kudos.base.data.xls

import java.io.File

/**
 * Excel data importer interface.
 *
 * @param T row-object type
 * @author
 * @since 1.0.0
 */
interface IExcelImporter<T> {

    /**
     * Performs the import.
     *
     * @param xlsFile the Excel file
     * @return list of imported row objects
     * @throws IllegalStateException when an error occurs during import
     * @author
     * @since 1.0.0
     */
    fun import(xlsFile: File): List<T>

}