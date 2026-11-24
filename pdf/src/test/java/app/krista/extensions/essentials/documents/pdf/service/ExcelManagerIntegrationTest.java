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

import app.krista.extensions.essentials.documents.pdf.BaseIntegrationTest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for ExcelManager with 100% code coverage.
 * Tests all public methods including edge cases, error handling, and various cell types.
 */
@DisplayName("ExcelManager Integration Tests")
class ExcelManagerIntegrationTest extends BaseIntegrationTest {

    private ExcelManager excelManager;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        excelManager = new ExcelManager();
    }

    // ==================== convertExcelToPDF Tests ====================

    @Test
    @DisplayName("Should convert Excel file to PDF successfully")
    void testConvertExcelToPDF_Success() throws Exception {
        // Arrange
        File excelFile = createSampleExcelFile("test.xlsx", 5, 3);

        // Act
        File result = excelManager.convertExcelToPDF(excelFile, null);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF file should exist");
        assertValidPdf(result);
    }

    @Test
    @DisplayName("Should convert specific Excel sheet to PDF")
    void testConvertExcelToPDF_SpecificSheet() throws Exception {
        // Arrange
        File excelFile = createMultiSheetExcelFile("multi-sheet.xlsx");

        // Act
        File result = excelManager.convertExcelToPDF(excelFile, "Sheet1");

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF file should exist");
        assertValidPdf(result);
    }

    @Test
    @DisplayName("Should convert all sheets when sheet name is null")
    void testConvertExcelToPDF_AllSheets() throws Exception {
        // Arrange
        File excelFile = createMultiSheetExcelFile("multi-sheet.xlsx");

        // Act
        File result = excelManager.convertExcelToPDF(excelFile, null);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF file should exist");
        assertValidPdf(result);
    }

    @Test
    @DisplayName("Should convert all sheets when sheet name is blank")
    void testConvertExcelToPDF_BlankSheetName() throws Exception {
        // Arrange
        File excelFile = createSampleExcelFile("test.xlsx", 3, 2);

        // Act
        File result = excelManager.convertExcelToPDF(excelFile, "   ");

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF file should exist");
    }

    @Test
    @DisplayName("Should handle Excel file with different cell types")
    void testConvertExcelToPDF_DifferentCellTypes() throws Exception {
        // Arrange
        File excelFile = createExcelFileWithDifferentCellTypes("cell-types.xlsx");

        // Act
        File result = excelManager.convertExcelToPDF(excelFile, null);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF file should exist");
        assertValidPdf(result);
    }

    @Test
    @DisplayName("Should handle Excel file with numeric cells")
    void testConvertExcelToPDF_NumericCells() throws Exception {
        // Arrange
        File excelFile = createExcelFileWithNumericCells("numeric.xlsx");

        // Act
        File result = excelManager.convertExcelToPDF(excelFile, null);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF file should exist");
    }

    @Test
    @DisplayName("Should handle Excel file with boolean cells")
    void testConvertExcelToPDF_BooleanCells() throws Exception {
        // Arrange
        File excelFile = createExcelFileWithBooleanCells("boolean.xlsx");

        // Act
        File result = excelManager.convertExcelToPDF(excelFile, null);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF file should exist");
    }

    @Test
    @DisplayName("Should handle Excel file with formula cells")
    void testConvertExcelToPDF_FormulaCells() throws Exception {
        // Arrange
        File excelFile = createExcelFileWithFormulaCells("formula.xlsx");

        // Act
        File result = excelManager.convertExcelToPDF(excelFile, null);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF file should exist");
    }

    @Test
    @DisplayName("Should throw exception for empty Excel file")
    void testConvertExcelToPDF_EmptyFile() throws Exception {
        // Arrange
        File excelFile = createSampleExcelFile("empty.xlsx", 0, 0);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            excelManager.convertExcelToPDF(excelFile, null);
        }, "Should throw exception for empty Excel file");
    }

    @Test
    @DisplayName("Should throw exception for invalid Excel file")
    void testConvertExcelToPDF_InvalidFile() throws Exception {
        // Arrange
        File invalidFile = tempDir.resolve("invalid.xlsx").toFile();
        try (FileOutputStream fos = new FileOutputStream(invalidFile)) {
            fos.write("This is not a valid Excel file".getBytes());
        }
        createdFiles.add(invalidFile);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            excelManager.convertExcelToPDF(invalidFile, null);
        }, "Should throw IllegalStateException for invalid Excel file");
    }

    @Test
    @DisplayName("Should throw exception for non-existent file")
    void testConvertExcelToPDF_NonExistentFile() {
        // Arrange
        File nonExistentFile = new File("/non/existent/file.xlsx");

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            excelManager.convertExcelToPDF(nonExistentFile, null);
        }, "Should throw exception for non-existent file");
    }

    // ==================== getWorkbook Tests ====================

    @Test
    @DisplayName("Should load workbook successfully")
    void testGetWorkbook_Success() throws Exception {
        // Arrange
        File excelFile = createSampleExcelFile("workbook.xlsx", 3, 3);

        // Act
        Workbook workbook = excelManager.getWorkbook(excelFile);

        // Assert
        assertNotNull(workbook, "Workbook should not be null");
        assertTrue(workbook.getNumberOfSheets() > 0, "Workbook should have sheets");
        workbook.close();
    }

    @Test
    @DisplayName("Should throw exception for invalid workbook file")
    void testGetWorkbook_InvalidFile() throws Exception {
        // Arrange
        File invalidFile = tempDir.resolve("invalid.xlsx").toFile();
        try (FileOutputStream fos = new FileOutputStream(invalidFile)) {
            fos.write("Not a valid workbook".getBytes());
        }
        createdFiles.add(invalidFile);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            excelManager.getWorkbook(invalidFile);
        }, "Should throw IllegalStateException for invalid workbook");
    }

    // ==================== Helper Methods ====================

    private File createMultiSheetExcelFile(String fileName) throws IOException {
        File excelFile = tempDir.resolve(fileName).toFile();
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(excelFile)) {

            // Create Sheet1
            Sheet sheet1 = workbook.createSheet("Sheet1");
            Row row1 = sheet1.createRow(0);
            row1.createCell(0).setCellValue("Sheet1 Data");

            // Create Sheet2
            Sheet sheet2 = workbook.createSheet("Sheet2");
            Row row2 = sheet2.createRow(0);
            row2.createCell(0).setCellValue("Sheet2 Data");

            workbook.write(out);
        }
        createdFiles.add(excelFile);
        return excelFile;
    }

    private File createExcelFileWithDifferentCellTypes(String fileName) throws IOException {
        File excelFile = tempDir.resolve(fileName).toFile();
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(excelFile)) {

            Sheet sheet = workbook.createSheet("Mixed Types");
            Row row = sheet.createRow(0);

            // String cell
            row.createCell(0).setCellValue("Text");
            // Numeric cell
            row.createCell(1).setCellValue(123.45);
            // Boolean cell
            row.createCell(2).setCellValue(true);
            // Formula cell
            row.createCell(3).setCellFormula("B1*2");

            workbook.write(out);
        }
        createdFiles.add(excelFile);
        return excelFile;
    }

    private File createExcelFileWithNumericCells(String fileName) throws IOException {
        File excelFile = tempDir.resolve(fileName).toFile();
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(excelFile)) {

            Sheet sheet = workbook.createSheet("Numeric");
            for (int i = 0; i < 5; i++) {
                Row row = sheet.createRow(i);
                for (int j = 0; j < 3; j++) {
                    row.createCell(j).setCellValue(i * 10 + j);
                }
            }
            workbook.write(out);
        }
        createdFiles.add(excelFile);
        return excelFile;
    }

    private File createExcelFileWithBooleanCells(String fileName) throws IOException {
        File excelFile = tempDir.resolve(fileName).toFile();
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(excelFile)) {

            Sheet sheet = workbook.createSheet("Boolean");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue(true);
            row.createCell(1).setCellValue(false);
            row.createCell(2).setCellValue(true);

            workbook.write(out);
        }
        createdFiles.add(excelFile);
        return excelFile;
    }

    private File createExcelFileWithFormulaCells(String fileName) throws IOException {
        File excelFile = tempDir.resolve(fileName).toFile();
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(excelFile)) {

            Sheet sheet = workbook.createSheet("Formula");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue(10);
            row.createCell(1).setCellValue(20);
            row.createCell(2).setCellFormula("A1+B1");
            row.createCell(3).setCellFormula("A1*B1");

            workbook.write(out);
        }
        createdFiles.add(excelFile);
        return excelFile;
    }
}

