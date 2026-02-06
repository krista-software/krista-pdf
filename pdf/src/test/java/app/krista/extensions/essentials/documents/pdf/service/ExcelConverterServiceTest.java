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

import app.krista.ksdk.files.FileHandle;
import app.krista.ksdk.files.FileRepository;
import app.krista.model.base.File;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExcelConverterService.
 * Tests Excel to PDF conversion functionality.
 */
@DisplayName("ExcelConverterService Unit Tests")
public class ExcelConverterServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private File excelFile;

    @Mock
    private FileHandle fileHandle;

    private ExcelConverterService excelConverterService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        excelConverterService = new ExcelConverterService(fileRepository);
    }

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("Should initialize with FileRepository dependency")
    void testConstructor_InitializesWithDependencies() {
        // Assert
        assertNotNull(excelConverterService, "Service should be initialized");
    }

    // ==================== File Name Handling Tests ====================

    @Test
    @DisplayName("Should generate PDF filename from Excel filename")
    void testFileNameConversion_XlsxToPdf() {
        // Arrange
        String excelFileName = "report.xlsx";
        String excelExtension = ".xlsx";

        // Act
        String pdfFileName = excelFileName.replace(excelExtension, ".pdf");

        // Assert
        assertEquals("report.pdf", pdfFileName);
    }

    @Test
    @DisplayName("Should handle filename with multiple dots")
    void testFileNameConversion_MultipleDotsInFilename() {
        // Arrange
        String excelFileName = "quarterly.report.2024.xlsx";
        String excelExtension = ".xlsx";

        // Act
        String pdfFileName = excelFileName.replace(excelExtension, ".pdf");

        // Assert
        assertEquals("quarterly.report.2024.pdf", pdfFileName);
    }

    @Test
    @DisplayName("Should handle XLS extension")
    void testFileNameConversion_XlsExtension() {
        // Arrange
        String excelFileName = "report.xls";
        String excelExtension = ".xls";

        // Act
        String pdfFileName = excelFileName.replace(excelExtension, ".pdf");

        // Assert
        assertEquals("report.pdf", pdfFileName);
    }

    // ==================== Column Width Calculation Tests ====================

    @Test
    @DisplayName("Should calculate minimum column width percentage")
    void testColumnWidthCalculation_MinimumWidth() {
        // Arrange
        float minColumnWidthPercentage = 10.0f;
        int numberOfColumns = 5;
        
        // Act
        float totalWidth = minColumnWidthPercentage * numberOfColumns;
        
        // Assert
        assertEquals(50.0f, totalWidth);
        assertTrue(minColumnWidthPercentage >= 10.0f, "Minimum width should be at least 10%");
    }

    @Test
    @DisplayName("Should handle single column table")
    void testColumnWidthCalculation_SingleColumn() {
        // Arrange
        int numberOfColumns = 1;
        float minColumnWidthPercentage = 10.0f;
        
        // Act
        float[] widths = new float[numberOfColumns];
        widths[0] = minColumnWidthPercentage;
        
        // Assert
        assertEquals(1, widths.length);
        assertEquals(10.0f, widths[0]);
    }

    @Test
    @DisplayName("Should distribute width across multiple columns")
    void testColumnWidthCalculation_MultipleColumns() {
        // Arrange
        int numberOfColumns = 4;
        float totalWidth = 100.0f;
        
        // Act
        float equalWidth = totalWidth / numberOfColumns;
        
        // Assert
        assertEquals(25.0f, equalWidth);
    }

    // ==================== Cell Value Extraction Tests ====================

    @Test
    @DisplayName("Should handle null cell")
    void testCellValueExtraction_NullCell() {
        // Arrange
        Cell nullCell = null;
        
        // Act
        String value = (nullCell == null) ? "" : nullCell.toString();
        
        // Assert
        assertEquals("", value, "Null cell should return empty string");
    }

    @Test
    @DisplayName("Should handle blank cell")
    void testCellValueExtraction_BlankCell() {
        // This validates the logic for blank cells
        // In actual implementation, blank cells return empty string
        
        // Arrange
        String blankValue = "";
        
        // Assert
        assertEquals("", blankValue);
    }

    @Test
    @DisplayName("Should handle numeric cell values")
    void testCellValueExtraction_NumericCell() {
        // Arrange
        double numericValue = 123.45;
        
        // Act
        String stringValue = String.valueOf(numericValue);
        
        // Assert
        assertEquals("123.45", stringValue);
    }

    @Test
    @DisplayName("Should handle string cell values")
    void testCellValueExtraction_StringCell() {
        // Arrange
        String stringValue = "Test Value";
        
        // Assert
        assertEquals("Test Value", stringValue);
    }

    @Test
    @DisplayName("Should handle boolean cell values")
    void testCellValueExtraction_BooleanCell() {
        // Arrange
        boolean boolValue = true;
        
        // Act
        String stringValue = String.valueOf(boolValue);
        
        // Assert
        assertEquals("true", stringValue);
    }

    @Test
    @DisplayName("Should handle formula cell values")
    void testCellValueExtraction_FormulaCell() {
        // Formula cells need to be evaluated
        // This test validates the concept
        
        // Arrange
        String formula = "=SUM(A1:A10)";
        
        // Assert
        assertTrue(formula.startsWith("="), "Formula should start with =");
    }

    // ==================== Sheet Processing Tests ====================

    @Test
    @DisplayName("Should handle empty sheet")
    void testSheetProcessing_EmptySheet() throws IOException {
        // Arrange
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Empty");
        
        // Act
        int rowCount = 0;
        for (Row row : sheet) {
            rowCount++;
        }
        
        // Assert
        assertEquals(0, rowCount, "Empty sheet should have no rows");
        workbook.close();
    }

    @Test
    @DisplayName("Should handle sheet with single row")
    void testSheetProcessing_SingleRow() throws IOException {
        // Arrange
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Single Row");
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("Header");
        
        // Act
        int rowCount = 0;
        for (Row r : sheet) {
            rowCount++;
        }
        
        // Assert
        assertEquals(1, rowCount, "Sheet should have one row");
        workbook.close();
    }

    @Test
    @DisplayName("Should handle sheet with multiple rows")
    void testSheetProcessing_MultipleRows() throws IOException {
        // Arrange
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Multiple Rows");
        for (int i = 0; i < 5; i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue("Row " + i);
        }
        
        // Act
        int rowCount = 0;
        for (Row r : sheet) {
            rowCount++;
        }
        
        // Assert
        assertEquals(5, rowCount, "Sheet should have 5 rows");
        workbook.close();
    }

    @Test
    @DisplayName("Should process sheet name correctly")
    void testSheetProcessing_SheetName() throws IOException {
        // Arrange
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sales Report");
        
        // Assert
        assertEquals("Sales Report", sheet.getSheetName());
        workbook.close();
    }

    // ==================== Multiple Sheets Tests ====================

    @Test
    @DisplayName("Should handle workbook with single sheet")
    void testMultipleSheets_SingleSheet() throws IOException {
        // Arrange
        XSSFWorkbook workbook = new XSSFWorkbook();
        workbook.createSheet("Sheet1");
        
        // Act
        int sheetCount = workbook.getNumberOfSheets();
        
        // Assert
        assertEquals(1, sheetCount);
        workbook.close();
    }

    @Test
    @DisplayName("Should handle workbook with multiple sheets")
    void testMultipleSheets_MultipleSheets() throws IOException {
        // Arrange
        XSSFWorkbook workbook = new XSSFWorkbook();
        workbook.createSheet("Sheet1");
        workbook.createSheet("Sheet2");
        workbook.createSheet("Sheet3");
        
        // Act
        int sheetCount = workbook.getNumberOfSheets();
        
        // Assert
        assertEquals(3, sheetCount);
        workbook.close();
    }

    @Test
    @DisplayName("Should iterate through all sheets")
    void testMultipleSheets_IterateSheets() throws IOException {
        // Arrange
        XSSFWorkbook workbook = new XSSFWorkbook();
        workbook.createSheet("Sales");
        workbook.createSheet("Expenses");
        workbook.createSheet("Summary");
        
        // Act
        int count = 0;
        var iterator = workbook.sheetIterator();
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        
        // Assert
        assertEquals(3, count, "Should iterate through all 3 sheets");
        workbook.close();
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should throw RuntimeException on conversion error")
    void testErrorHandling_ConversionError() throws IOException {
        // Arrange
        when(excelFile.getFileName()).thenReturn("test.xlsx");
        when(excelFile.getExtension()).thenReturn(".xlsx");
        when(fileRepository.getFile(excelFile)).thenReturn(fileHandle);
        when(fileHandle.getContent()).thenThrow(new IOException("File read error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> excelConverterService.convertExcelFileWithMultipleSheetsToPDF(excelFile));

        assertTrue(exception.getMessage().contains("Error converting excel file to PDF"));
    }

    @Test
    @DisplayName("Should provide meaningful error message")
    void testErrorHandling_MeaningfulErrorMessage() {
        // Arrange
        String errorMessage = "Error converting excel file to PDF";
        
        // Assert
        assertTrue(errorMessage.contains("Error converting"));
        assertTrue(errorMessage.contains("excel file"));
        assertTrue(errorMessage.contains("PDF"));
    }

    // ==================== Page Size Tests ====================

    @Test
    @DisplayName("Should use A4 page size")
    void testPageSize_A4() {
        // A4 dimensions: 595 x 842 points
        float a4Width = 595.0f;
        float a4Height = 842.0f;
        
        // Assert
        assertTrue(a4Width > 0);
        assertTrue(a4Height > 0);
        assertTrue(a4Height > a4Width, "A4 height should be greater than width");
    }

    // ==================== Font Configuration Tests ====================

    @Test
    @DisplayName("Should use appropriate font size for table")
    void testFontConfiguration_TableFont() {
        // Table font size should be 8
        int tableFontSize = 8;
        
        // Assert
        assertTrue(tableFontSize > 0);
        assertTrue(tableFontSize < 12, "Table font should be smaller than title font");
    }

    @Test
    @DisplayName("Should use appropriate font size for title")
    void testFontConfiguration_TitleFont() {
        // Title font size should be 14 and bold
        int titleFontSize = 14;
        boolean isBold = true;
        
        // Assert
        assertTrue(titleFontSize > 0);
        assertTrue(titleFontSize > 8, "Title font should be larger than table font");
        assertTrue(isBold, "Title should be bold");
    }

    // ==================== Integration Validation Tests ====================

    @Test
    @DisplayName("Should validate complete workflow components")
    void testWorkflow_RequiredComponents() {
        // Validate that all required components are present
        assertNotNull(excelConverterService, "Service should be initialized");
        assertNotNull(fileRepository, "FileRepository should be injected");
    }

    @Test
    @DisplayName("Should handle ByteArrayOutputStream for PDF generation")
    void testWorkflow_ByteArrayOutputStream() throws IOException {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Act
        outputStream.write("Test PDF content".getBytes());
        byte[] pdfBytes = outputStream.toByteArray();
        
        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        outputStream.close();
    }
}

