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

package app.krista.extensions.essentials.documents.pdf.helper;

import app.krista.extensions.essentials.documents.pdf.service.PDFManager;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration tests for TableDataPdfHelper.
 * Tests PDF generation from table data with various scenarios.
 */
@DisplayName("TableDataPdfHelper Integration Tests")
public class TableDataPdfHelperIntegrationTest {

    @Mock
    private PDFManager pdfManager;

    @Mock
    private KristaMediaClientUtil kristaMediaClientUtil;

    @Mock
    private TelemetryHelper telemetryHelper;

    private TableDataPdfHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        helper = new TableDataPdfHelper(
                pdfManager,
                kristaMediaClientUtil,
                telemetryHelper
        );
    }

    // ==================== Successful PDF Generation Tests ====================

    @Test
    @DisplayName("Should successfully generate PDF from table data with all parameters")
    void testGetTableDataPDF_AllParameters_Success() throws Exception {
        // Arrange
        String title = "Sales Report";
        String subTitle = "Q4 2024";
        String description = "Quarterly sales data";
        String header = "Company Header";
        String footer = "Page Footer";

        File logoFile = mock(File.class);
        java.io.File downloadedLogo = mock(java.io.File.class);

        List<Map<String, Object>> tableData = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("Product", "Widget A");
        row1.put("Sales", 1000);
        tableData.add(row1);

        java.io.File generatedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(kristaMediaClientUtil.downloadFileHandler(logoFile)).thenReturn(downloadedLogo);
        when(pdfManager.getTableDataPdf(title, subTitle, description, header, footer, downloadedLogo, tableData))
                .thenReturn(generatedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(generatedPdf)).thenReturn(uploadedFile);

        // Act
        File result = helper.getTableDataPDF(title, subTitle, description, header, footer, logoFile, tableData);

        // Assert
        assertNotNull(result);
        assertEquals(uploadedFile, result);
        verify(pdfManager).getTableDataPdf(title, subTitle, description, header, footer, downloadedLogo, tableData);
        verify(kristaMediaClientUtil).uploadFileHandler(generatedPdf);
        verify(telemetryHelper).incrementCount("pdf.getTableDataPDF");
        verify(telemetryHelper).recordSuccess(eq("pdf.getTableDataPDF"), anyLong(), any());
    }

    @Test
    @DisplayName("Should successfully generate PDF with minimal parameters")
    void testGetTableDataPDF_MinimalParameters_Success() throws Exception {
        // Arrange
        List<Map<String, Object>> tableData = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("Name", "Test");
        tableData.add(row);

        java.io.File generatedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(pdfManager.getTableDataPdf(null, null, null, null, null, null, tableData))
                .thenReturn(generatedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(generatedPdf)).thenReturn(uploadedFile);

        // Act
        File result = helper.getTableDataPDF(null, null, null, null, null, null, tableData);

        // Assert
        assertNotNull(result);
        verify(pdfManager).getTableDataPdf(null, null, null, null, null, null, tableData);
    }

    @Test
    @DisplayName("Should successfully generate PDF with multiple rows")
    void testGetTableDataPDF_MultipleRows_Success() throws Exception {
        // Arrange
        List<Map<String, Object>> tableData = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("ID", i);
            row.put("Name", "Item " + i);
            tableData.add(row);
        }

        java.io.File generatedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(pdfManager.getTableDataPdf(anyString(), any(), any(), any(), any(), any(), eq(tableData)))
                .thenReturn(generatedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(generatedPdf)).thenReturn(uploadedFile);

        // Act
        File result = helper.getTableDataPDF("Report", null, null, null, null, null, tableData);

        // Assert
        assertNotNull(result);
        verify(telemetryHelper).recordSuccess(eq("pdf.getTableDataPDF"), anyLong(), any());
    }

    // ==================== Null/Empty Input Tests ====================

    @Test
    @DisplayName("Should throw exception when table data is null")
    void testGetTableDataPDF_NullTableData() throws Exception {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            helper.getTableDataPDF("Title", null, null, null, null, null, null);
        });

        assertEquals("Please provide table data", exception.getMessage());
        verify(pdfManager, never()).getTableDataPdf(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when table data is empty")
    void testGetTableDataPDF_EmptyTableData() throws Exception {
        // Arrange
        List<Map<String, Object>> emptyData = new ArrayList<>();

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            helper.getTableDataPDF("Title", null, null, null, null, null, emptyData);
        });

        assertEquals("Please provide table data", exception.getMessage());
    }

    // ==================== Exception Handling Tests ====================

    @Test
    @DisplayName("Should handle exception during PDF generation")
    void testGetTableDataPDF_GenerationException() throws Exception {
        // Arrange
        List<Map<String, Object>> tableData = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("Name", "Test");
        tableData.add(row);

        when(pdfManager.getTableDataPdf(any(), any(), any(), any(), any(), any(), eq(tableData)))
                .thenThrow(new RuntimeException("PDF generation failed"));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            helper.getTableDataPDF("Title", null, null, null, null, null, tableData);
        });

        assertEquals("PDF generation failed", exception.getMessage());
        verify(telemetryHelper).recordError(eq("pdf.getTableDataPDF"), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should handle exception during file upload")
    void testGetTableDataPDF_UploadException() throws Exception {
        // Arrange
        List<Map<String, Object>> tableData = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("Name", "Test");
        tableData.add(row);

        java.io.File generatedPdf = mock(java.io.File.class);

        when(pdfManager.getTableDataPdf(any(), any(), any(), any(), any(), any(), eq(tableData)))
                .thenReturn(generatedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(generatedPdf))
                .thenThrow(new RuntimeException("Upload failed"));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            helper.getTableDataPDF("Title", null, null, null, null, null, tableData);
        });

        assertEquals("Upload failed", exception.getMessage());
        verify(telemetryHelper).recordError(eq("pdf.getTableDataPDF"), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should handle exception during logo download")
    void testGetTableDataPDF_LogoDownloadException() throws Exception {
        // Arrange
        File logoFile = mock(File.class);
        List<Map<String, Object>> tableData = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("Name", "Test");
        tableData.add(row);

        when(kristaMediaClientUtil.downloadFileHandler(logoFile))
                .thenThrow(new RuntimeException("Logo download failed"));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            helper.getTableDataPDF("Title", null, null, null, null, logoFile, tableData);
        });

        assertEquals("Logo download failed", exception.getMessage());
        verify(telemetryHelper).recordError(eq("pdf.getTableDataPDF"), anyLong(), any(), any());
    }

    // ==================== Telemetry Tests ====================

    @Test
    @DisplayName("Should record telemetry for successful operation")
    void testGetTableDataPDF_TelemetrySuccess() throws Exception {
        // Arrange
        List<Map<String, Object>> tableData = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("Name", "Test");
        tableData.add(row);

        java.io.File generatedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(pdfManager.getTableDataPdf(any(), any(), any(), any(), any(), any(), eq(tableData)))
                .thenReturn(generatedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(generatedPdf)).thenReturn(uploadedFile);

        // Act
        helper.getTableDataPDF("Test Title", null, null, null, null, null, tableData);

        // Assert
        verify(telemetryHelper).incrementCount("pdf.getTableDataPDF");
        verify(telemetryHelper).recordSuccess(eq("pdf.getTableDataPDF"), anyLong(), any());
    }

    @Test
    @DisplayName("Should record telemetry for failed operation")
    void testGetTableDataPDF_TelemetryFailure() throws Exception {
        // Arrange
        List<Map<String, Object>> tableData = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("Name", "Test");
        tableData.add(row);

        when(pdfManager.getTableDataPdf(any(), any(), any(), any(), any(), any(), eq(tableData)))
                .thenThrow(new RuntimeException("Failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            helper.getTableDataPDF("Title", null, null, null, null, null, tableData);
        });

        verify(telemetryHelper).incrementCount("pdf.getTableDataPDF");
        verify(telemetryHelper).recordError(eq("pdf.getTableDataPDF"), anyLong(), any(), any());
        verify(telemetryHelper, never()).recordSuccess(anyString(), anyLong(), any());
    }
}


