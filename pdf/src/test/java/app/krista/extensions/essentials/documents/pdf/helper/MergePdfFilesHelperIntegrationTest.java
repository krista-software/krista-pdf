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

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extensions.essentials.documents.pdf.catalog.generartors.ExtensionResponseGenerator;
import app.krista.extensions.essentials.documents.pdf.catalog.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.SubCatalogConstants;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.documents.pdf.service.PDFManager;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration tests for MergePdfFilesHelper.
 */
@DisplayName("MergePdfFilesHelper Integration Tests")
public class MergePdfFilesHelperIntegrationTest {

    @Mock
    private PDFManager pdfManager;
    @Mock
    private KristaMediaClientUtil kristaMediaClientUtil;
    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private ValidationOrchestrator validationOrchestrator;
    @Mock
    private ErrorHandlingStateManager errorHandlingStateManager;
    @Mock
    private ExtensionResponseGenerator responseGenerator;

    private MergePdfFilesHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        helper = new MergePdfFilesHelper(
                pdfManager,
                kristaMediaClientUtil,
                telemetryHelper,
                validationOrchestrator,
                errorHandlingStateManager,
                responseGenerator
        );
    }

    @Test
    @DisplayName("Should successfully merge PDF files with all parameters")
    void testMergePdfFiles_AllParameters_Success() throws Exception {
        // Arrange
        File file1 = mock(File.class);
        File file2 = mock(File.class);
        when(file1.getFileName()).thenReturn("file1.pdf");
        when(file2.getFileName()).thenReturn("file2.pdf");
        List<File> inputFiles = Arrays.asList(file1, file2);

        File logoFile = mock(File.class);
        java.io.File downloadedLogo = mock(java.io.File.class);
        java.io.File downloadedFile1 = mock(java.io.File.class);
        java.io.File downloadedFile2 = mock(java.io.File.class);
        java.io.File mergedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(logoFile)).thenReturn(downloadedLogo);
        when(kristaMediaClientUtil.downloadFileHandler(file1)).thenReturn(downloadedFile1);
        when(kristaMediaClientUtil.downloadFileHandler(file2)).thenReturn(downloadedFile2);
        when(pdfManager.mergePdfFiles(anyString(), anyString(), anyString(), anyString(), anyString(),
                eq(downloadedLogo), anyList())).thenReturn(mergedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(mergedPdf)).thenReturn(uploadedFile);

        // Act
        ExtensionResponse response = helper.mergePdfFiles("Title", "Subtitle", "Description",
                "Header", "Footer", logoFile, inputFiles, false);

        // Assert
        assertNotNull(response);
        verify(pdfManager).mergePdfFiles(eq("Title"), eq("Subtitle"), eq("Description"),
                eq("Header"), eq("Footer"), eq(downloadedLogo), anyList());
        verify(telemetryHelper).incrementCount("pdf.mergePdfFiles");
        verify(telemetryHelper).recordSuccess(eq("pdf.mergePdfFiles"), anyLong(), any());
    }

    @Test
    @DisplayName("Should successfully merge PDF files with minimal parameters")
    void testMergePdfFiles_MinimalParameters_Success() throws Exception {
        // Arrange
        File file1 = mock(File.class);
        File file2 = mock(File.class);
        when(file1.getFileName()).thenReturn("file1.pdf");
        when(file2.getFileName()).thenReturn("file2.pdf");
        List<File> inputFiles = Arrays.asList(file1, file2);

        java.io.File downloadedFile1 = mock(java.io.File.class);
        java.io.File downloadedFile2 = mock(java.io.File.class);
        java.io.File mergedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(file1)).thenReturn(downloadedFile1);
        when(kristaMediaClientUtil.downloadFileHandler(file2)).thenReturn(downloadedFile2);
        when(pdfManager.mergePdfFiles(isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyList())).thenReturn(mergedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(mergedPdf)).thenReturn(uploadedFile);

        // Act
        ExtensionResponse response = helper.mergePdfFiles(null, null, null, null, null, null, inputFiles, false);

        // Assert
        assertNotNull(response);
        verify(pdfManager).mergePdfFiles(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyList());
    }

    @Test
    @DisplayName("Should handle null input files")
    void testMergePdfFiles_NullInputFiles() throws Exception {
        // Act
        ExtensionResponse response = helper.mergePdfFiles("Title", null, null, null, null, null, null, false);

        // Assert
        assertNotNull(response);
        verify(pdfManager, never()).mergePdfFiles(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle empty input files")
    void testMergePdfFiles_EmptyInputFiles() throws Exception {
        // Act
        ExtensionResponse response = helper.mergePdfFiles("Title", null, null, null, null, null, new ArrayList<>(), false);

        // Assert
        assertNotNull(response);
        verify(pdfManager, never()).mergePdfFiles(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle file count validation failure with retry disabled")
    void testMergePdfFiles_FileCountValidationFailure_NoRetry() {
        // Arrange
        File file1 = mock(File.class);
        when(file1.getFileName()).thenReturn("file1.pdf");
        List<File> inputFiles = Collections.singletonList(file1);

        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult(
                        "Confirm", "Field", "Fetch", "Error: need at least 2 files", "Type"
                );
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);

        when(validationOrchestrator.validate(any())).thenReturn(validationResults);
        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.mergePdfFiles("Title", null, null, null, null, null, inputFiles, false);

        // Assert
        assertNotNull(response);
        verify(responseGenerator).generateValidationErrorResponse(validationResults);
    }

    @Test
    @DisplayName("Should handle file format validation failure with retry enabled")
    void testMergePdfFiles_FileFormatValidationFailure_WithRetry() {
        // Arrange
        File file1 = mock(File.class);
        File file2 = mock(File.class);
        when(file1.getFileName()).thenReturn("file1.txt");
        when(file2.getFileName()).thenReturn("file2.pdf");
        List<File> inputFiles = Arrays.asList(file1, file2);

        // First validation passes (file count), second fails (file format)
        when(validationOrchestrator.validate(any()))
                .thenReturn(new ArrayList<>())
                .thenReturn(Collections.singletonList(
                        new ValidationOrchestrator.ValidationResult(
                                "Confirm", "Field", "Fetch", "Error: invalid format", "Type"
                        )
                ));

        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                any(),
                eq(SubCatalogConstants.CONFIRM_PDF_MERGED_FORMAT_VALIDATION),
                any()
        )).thenReturn(confirmationResponse);

        // Act
        ExtensionResponse response = helper.mergePdfFiles("Title", null, null, null, null, null, inputFiles, true);

        // Assert
        assertNotNull(response);
        verify(errorHandlingStateManager).put(anyString(), any());
    }

    @Test
    @DisplayName("Should handle exception during PDF merging")
    void testMergePdfFiles_MergingException() throws Exception {
        // Arrange
        File file1 = mock(File.class);
        File file2 = mock(File.class);
        when(file1.getFileName()).thenReturn("file1.pdf");
        when(file2.getFileName()).thenReturn("file2.pdf");
        List<File> inputFiles = Arrays.asList(file1, file2);

        java.io.File downloadedFile1 = mock(java.io.File.class);
        java.io.File downloadedFile2 = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(file1)).thenReturn(downloadedFile1);
        when(kristaMediaClientUtil.downloadFileHandler(file2)).thenReturn(downloadedFile2);
        when(pdfManager.mergePdfFiles(any(), any(), any(), any(), any(), any(), anyList()))
                .thenThrow(new RuntimeException("Merge failed"));

        // Act
        ExtensionResponse response = helper.mergePdfFiles("Title", null, null, null, null, null, inputFiles, false);

        // Assert
        assertNotNull(response);
        verify(telemetryHelper).recordError(eq("pdf.mergePdfFiles"), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should record telemetry for successful operation")
    void testMergePdfFiles_TelemetrySuccess() throws Exception {
        // Arrange
        File file1 = mock(File.class);
        File file2 = mock(File.class);
        when(file1.getFileName()).thenReturn("file1.pdf");
        when(file2.getFileName()).thenReturn("file2.pdf");
        List<File> inputFiles = Arrays.asList(file1, file2);

        java.io.File downloadedFile1 = mock(java.io.File.class);
        java.io.File downloadedFile2 = mock(java.io.File.class);
        java.io.File mergedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(file1)).thenReturn(downloadedFile1);
        when(kristaMediaClientUtil.downloadFileHandler(file2)).thenReturn(downloadedFile2);
        when(pdfManager.mergePdfFiles(any(), any(), any(), any(), any(), any(), anyList())).thenReturn(mergedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(mergedPdf)).thenReturn(uploadedFile);

        // Act
        helper.mergePdfFiles("Title", null, null, null, null, null, inputFiles, false);

        // Assert
        verify(telemetryHelper).incrementCount("pdf.mergePdfFiles");
        verify(telemetryHelper).recordSuccess(eq("pdf.mergePdfFiles"), anyLong(), any());
    }

    @Test
    @DisplayName("Should record telemetry for failed operation")
    void testMergePdfFiles_TelemetryFailure() throws Exception {
        // Arrange
        File file1 = mock(File.class);
        File file2 = mock(File.class);
        when(file1.getFileName()).thenReturn("file1.pdf");
        when(file2.getFileName()).thenReturn("file2.pdf");
        List<File> inputFiles = Arrays.asList(file1, file2);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(any()))
                .thenThrow(new RuntimeException("Download failed"));

        // Act
        helper.mergePdfFiles("Title", null, null, null, null, null, inputFiles, false);

        // Assert
        verify(telemetryHelper).incrementCount("pdf.mergePdfFiles");
        verify(telemetryHelper).recordError(eq("pdf.mergePdfFiles"), anyLong(), any(), any());
        verify(telemetryHelper, never()).recordSuccess(anyString(), anyLong(), any());
    }
}


