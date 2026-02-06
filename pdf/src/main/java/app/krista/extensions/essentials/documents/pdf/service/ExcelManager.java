/*
 * Pdf Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>. 
 */

package app.krista.extensions.essentials.documents.pdf.service;

import app.krista.extensions.essentials.documents.pdf.util.PDFHelper;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

@Service
public class ExcelManager {

    private static final Logger logger = LoggerFactory.getLogger(ExcelManager.class);

    /**
     * Converts an Excel file to PDF format.
     * This method takes an Excel file and converts it to PDF, either converting a specific
     * sheet (if sheetName is provided) or all sheets in the workbook. The resulting PDF
     * uses landscape A4 page size to accommodate typical spreadsheet layouts.
     *
     * @param file The input Excel file to convert (supports .xlsx and .xls formats)
     * @param sheetName The name of the specific sheet to convert, or null to convert all sheets
     * @return A File object representing the generated PDF file
     * @throws IllegalStateException if the conversion fails due to I/O or processing errors
     */
    public File convertExcelToPDF(File file, String sheetName) {

        try (XSSFWorkbook xssfWorkbook = (XSSFWorkbook) WorkbookFactory.create(file, null, false)) {
            String pdfFilePath = PDFHelper.generatePdfFilePath(file.getName());
            Document iTextXlsToPdf = new Document(PageSize.A4.rotate());
            writeToPdf(sheetName, xssfWorkbook, pdfFilePath, iTextXlsToPdf);
            return new File(pdfFilePath);
        } catch (Exception cause) {
            throw new IllegalStateException("Failed to generate PDF " + cause.getMessage(), cause);
        }
    }

    private void writeToPdf(String sheetName, XSSFWorkbook xssfWorkbook, String pdfFilePath, Document iTextXlsToPdf) throws DocumentException, FileNotFoundException {
        XSSFSheet xssfSheet;
        PdfWriter.getInstance(iTextXlsToPdf, new FileOutputStream(pdfFilePath));
        iTextXlsToPdf.open();
        if (sheetName != null && !sheetName.isBlank()) {
            xssfSheet = xssfWorkbook.getSheet(sheetName);
            addTableToPdf(xssfSheet, getMaxColumnCount(xssfSheet), iTextXlsToPdf);
        } else {
            for (int sheetIndex = 0; sheetIndex < xssfWorkbook.getNumberOfSheets(); sheetIndex++) {
                xssfSheet = xssfWorkbook.getSheetAt(sheetIndex);
                addTableToPdf(xssfSheet, getMaxColumnCount(xssfSheet), iTextXlsToPdf);
            }
        }
        iTextXlsToPdf.close();
    }

    /**
     * This function will write excel data to pdf file
     *
     * @param xssfSheet            input {@link XSSFSheet} object of the given Excel file sheet
     * @param availableColumnCount number of columns in the given Excel sheet
     * @param iTextXlsToPdf        iText pdf document
     * @throws DocumentException  If an I/O error occurs.
     * @throws FileNotFoundException If the specified file is not found.
     */
    private void addTableToPdf(XSSFSheet xssfSheet, int availableColumnCount, Document iTextXlsToPdf) throws DocumentException {
        PdfPTable table = new PdfPTable(availableColumnCount);
        //We will use the object below to dynamically add new data to the table
        for (Row row : xssfSheet) {
            populatePdfTableFromExcelRow(availableColumnCount, table, row);
        }
        iTextXlsToPdf.add(table);
    }

    /**
     * This function will generate table row and add data to cell
     *
     * @param availableColumns number of columns in the Excel file
     * @param table            {@link PdfPTable} object to add date to table
     * @param row              {@link Row} object to add data to row of table
     */
    private void populatePdfTableFromExcelRow(int availableColumns, PdfPTable table, Row row) {
        PdfPCell tableCell;
        for (int cellNumber = 0; cellNumber < availableColumns; cellNumber++) {
            Cell cell = row.getCell(cellNumber, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            CellType cellType = cell.getCellType();
            tableCell = populateTableCell(cell, cellType);
            table.addCell(tableCell);
        }
    }

    /**
     * This function will generate a Table Cell data to be added to pdf {@link File} for given Excel sheet cell
     *
     * @param cell     input cell of the Excel Sheet
     * @param cellType type of cell data given
     * @return {@link PdfPCell} object to be added to pdf
     */
    private PdfPCell populateTableCell(Cell cell, CellType cellType) {
        PdfPCell tableCell;
        switch (cellType) {
            case NUMERIC:
                //Need to use DataFormatter to get actual number value
                DataFormatter fmt = new DataFormatter();
                String cellValue = fmt.formatCellValue(cell);
                tableCell = new PdfPCell(new Phrase(cellValue));
                break;
            case BOOLEAN:
                tableCell = new PdfPCell(new Phrase(String.valueOf(cell.getBooleanCellValue())));
                break;
            case FORMULA:
                tableCell = new PdfPCell(new Phrase(cell.getCellFormula()));
                break;
            default:
                //String is considered as default
                tableCell = new PdfPCell(new Phrase(cell.getStringCellValue()));
                break;
        }
        return tableCell;
    }

    /**
     * This function will return number of columns in the given Excel Sheet
     *
     * @param xssfSheet given input Excel {@link XSSFSheet} Sheet
     * @return number of columns in the Excel Sheet
     */
    private int getMaxColumnCount(XSSFSheet xssfSheet) {
        int columnCount = 0;
        for (Row row : xssfSheet) {
            int rowColNum = row.getLastCellNum();
            if (rowColNum > columnCount) {
                columnCount = rowColNum;
            }
        }
        return columnCount;
    }
    /**
     * Loads an Excel workbook from a given file.
     *
     * @param file the Excel file
     * @return the {@link Workbook} object
     */
    public Workbook getWorkbook(File file) {
        try {
            return WorkbookFactory.create(file, null, false);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read workbook: " + e.getMessage(), e);
        }
    }

}
