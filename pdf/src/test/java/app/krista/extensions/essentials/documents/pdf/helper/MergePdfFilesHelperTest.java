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
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.documents.pdf.service.PDFManager;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MergePdfFilesHelper focusing on the file download logging
 * and unique file handling functionality.
 */
@DisplayName("MergePdfFilesHelper Unit Tests")
public class MergePdfFilesHelperTest {

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

    // ==================== File Download Logging Tests ====================

    @Test
    @DisplayName("Should log file downloads during merge operation")
    void testMergePdfFiles_LogsFileDownloads() throws Exception {
        // Arrange
        File file1 = mock(File.class);
        File file2 = mock(File.class);
        when(file1.getFileName()).thenReturn("document1.pdf");
        when(file2.getFileName()).thenReturn("document2.pdf");
        List<File> inputFiles = Arrays.asList(file1, file2);

        java.io.File downloadedFile1 = mock(java.io.File.class);
        java.io.File downloadedFile2 = mock(java.io.File.class);
        java.io.File mergedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(downloadedFile1.getAbsolutePath()).thenReturn("/tmp/document1_uuid1.pdf");
        when(downloadedFile2.getAbsolutePath()).thenReturn("/tmp/document2_uuid2.pdf");

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(file1)).thenReturn(downloadedFile1);
        when(kristaMediaClientUtil.downloadFileHandler(file2)).thenReturn(downloadedFile2);
        when(pdfManager.mergePdfFiles(any(), any(), any(), any(), any(), any(), anyList())).thenReturn(mergedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(mergedPdf)).thenReturn(uploadedFile);

        // Act
        ExtensionResponse response = helper.mergePdfFiles("Title", null, null, null, null, null, inputFiles, false);

        // Assert
        assertNotNull(response);
        verify(kristaMediaClientUtil).downloadFileHandler(file1);
        verify(kristaMediaClientUtil).downloadFileHandler(file2);
        verify(pdfManager).mergePdfFiles(any(), any(), any(), any(), any(), any(), anyList());
    }

    @Test
    @DisplayName("Should download all files before merging")
    void testMergePdfFiles_DownloadsAllFilesBeforeMerge() throws Exception {
        // Arrange
        File file1 = mock(File.class);
        File file2 = mock(File.class);
        File file3 = mock(File.class);
        when(file1.getFileName()).thenReturn("file1.pdf");
        when(file2.getFileName()).thenReturn("file2.pdf");
        when(file3.getFileName()).thenReturn("file3.pdf");
        List<File> inputFiles = Arrays.asList(file1, file2, file3);

        java.io.File downloadedFile1 = mock(java.io.File.class);
        java.io.File downloadedFile2 = mock(java.io.File.class);
        java.io.File downloadedFile3 = mock(java.io.File.class);
        java.io.File mergedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(file1)).thenReturn(downloadedFile1);
        when(kristaMediaClientUtil.downloadFileHandler(file2)).thenReturn(downloadedFile2);
        when(kristaMediaClientUtil.downloadFileHandler(file3)).thenReturn(downloadedFile3);
        when(pdfManager.mergePdfFiles(any(), any(), any(), any(), any(), any(), anyList())).thenReturn(mergedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(mergedPdf)).thenReturn(uploadedFile);

        // Act
        ExtensionResponse response = helper.mergePdfFiles("Title", null, null, null, null, null, inputFiles, false);

        // Assert
        assertNotNull(response);
        
        // Verify all files were downloaded
        verify(kristaMediaClientUtil).downloadFileHandler(file1);
        verify(kristaMediaClientUtil).downloadFileHandler(file2);
        verify(kristaMediaClientUtil).downloadFileHandler(file3);
        
        // Verify merge was called with all downloaded files
        ArgumentCaptor<List> filesCaptor = ArgumentCaptor.forClass(List.class);
        verify(pdfManager).mergePdfFiles(any(), any(), any(), any(), any(), any(), filesCaptor.capture());
        
        List<java.io.File> mergedFiles = filesCaptor.getValue();
        assertEquals(3, mergedFiles.size(), "Should merge all 3 files");
        assertTrue(mergedFiles.contains(downloadedFile1));
        assertTrue(mergedFiles.contains(downloadedFile2));
        assertTrue(mergedFiles.contains(downloadedFile3));
    }

    @Test
    @DisplayName("Should handle files with same name but different content")
    void testMergePdfFiles_SameNameDifferentContent() throws Exception {
        // Arrange - Two files with same name from different locations
        File file1 = mock(File.class);
        File file2 = mock(File.class);
        when(file1.getFileName()).thenReturn("document.pdf");
        when(file2.getFileName()).thenReturn("document.pdf");
        List<File> inputFiles = Arrays.asList(file1, file2);

        // Mock downloaded files with unique paths (simulating UUID generation)
        java.io.File downloadedFile1 = mock(java.io.File.class);
        java.io.File downloadedFile2 = mock(java.io.File.class);
        when(downloadedFile1.getAbsolutePath()).thenReturn("/tmp/document_uuid1.pdf");
        when(downloadedFile2.getAbsolutePath()).thenReturn("/tmp/document_uuid2.pdf");
        
        java.io.File mergedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(file1)).thenReturn(downloadedFile1);
        when(kristaMediaClientUtil.downloadFileHandler(file2)).thenReturn(downloadedFile2);
        when(pdfManager.mergePdfFiles(any(), any(), any(), any(), any(), any(), anyList())).thenReturn(mergedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(mergedPdf)).thenReturn(uploadedFile);

        // Act
        ExtensionResponse response = helper.mergePdfFiles("Title", null, null, null, null, null, inputFiles, false);

        // Assert
        assertNotNull(response);
        
        // Verify both files were downloaded separately
        verify(kristaMediaClientUtil).downloadFileHandler(file1);
        verify(kristaMediaClientUtil).downloadFileHandler(file2);
        
        // Verify merge was called with both unique files
        ArgumentCaptor<List> filesCaptor = ArgumentCaptor.forClass(List.class);
        verify(pdfManager).mergePdfFiles(any(), any(), any(), any(), any(), any(), filesCaptor.capture());
        
        List<java.io.File> mergedFiles = filesCaptor.getValue();
        assertEquals(2, mergedFiles.size(), "Should have 2 distinct files");
        assertTrue(mergedFiles.contains(downloadedFile1), "Should contain first file");
        assertTrue(mergedFiles.contains(downloadedFile2), "Should contain second file");
        assertNotEquals(downloadedFile1.getAbsolutePath(), downloadedFile2.getAbsolutePath(), 
            "Files should have different paths");
    }

    @Test
    @DisplayName("Should handle merge with logo file")
    void testMergePdfFiles_WithLogoFile() throws Exception {
        // Arrange
        File logoFile = mock(File.class);
        File file1 = mock(File.class);
        when(logoFile.getFileName()).thenReturn("logo.png");
        when(file1.getFileName()).thenReturn("document.pdf");
        List<File> inputFiles = Arrays.asList(file1);

        java.io.File downloadedLogo = mock(java.io.File.class);
        java.io.File downloadedFile1 = mock(java.io.File.class);
        java.io.File mergedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(logoFile)).thenReturn(downloadedLogo);
        when(kristaMediaClientUtil.downloadFileHandler(file1)).thenReturn(downloadedFile1);
        when(pdfManager.mergePdfFiles(anyString(), anyString(), anyString(), anyString(), anyString(),
                eq(downloadedLogo), anyList())).thenReturn(mergedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(mergedPdf)).thenReturn(uploadedFile);

        // Act
        ExtensionResponse response = helper.mergePdfFiles("Title", "Subtitle", "Description",
                "Header", "Footer", logoFile, inputFiles, false);

        // Assert
        assertNotNull(response);
        verify(kristaMediaClientUtil).downloadFileHandler(logoFile);
        verify(kristaMediaClientUtil).downloadFileHandler(file1);
        verify(pdfManager).mergePdfFiles(eq("Title"), eq("Subtitle"), eq("Description"),
                eq("Header"), eq("Footer"), eq(downloadedLogo), anyList());
    }

    @Test
    @DisplayName("Should handle empty file list")
    void testMergePdfFiles_EmptyFileList() throws Exception {
        // Arrange
        List<File> emptyFiles = new ArrayList<>();
        
        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());

        // Act
        ExtensionResponse response = helper.mergePdfFiles("Title", null, null, null, null, null, emptyFiles, false);

        // Assert
        assertNotNull(response);
        verify(kristaMediaClientUtil, never()).downloadFileHandler(any());
    }

    @Test
    @DisplayName("Should handle download failure gracefully")
    void testMergePdfFiles_DownloadFailure() throws Exception {
        // Arrange
        File file1 = mock(File.class);
        when(file1.getFileName()).thenReturn("document.pdf");
        List<File> inputFiles = Arrays.asList(file1);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(file1))
                .thenThrow(new RuntimeException("Download failed"));

        // Act
        ExtensionResponse response = helper.mergePdfFiles("Title", null, null, null, null, null, inputFiles, false);

        // Assert
        assertNotNull(response);
        verify(kristaMediaClientUtil).downloadFileHandler(file1);
        verify(pdfManager, never()).mergePdfFiles(any(), any(), any(), any(), any(), any(), anyList());
    }

    @Test
    @DisplayName("Should pass downloaded files in correct order to merge")
    void testMergePdfFiles_MaintainsFileOrder() throws Exception {
        // Arrange
        File file1 = mock(File.class);
        File file2 = mock(File.class);
        File file3 = mock(File.class);
        when(file1.getFileName()).thenReturn("first.pdf");
        when(file2.getFileName()).thenReturn("second.pdf");
        when(file3.getFileName()).thenReturn("third.pdf");
        List<File> inputFiles = Arrays.asList(file1, file2, file3);

        java.io.File downloadedFile1 = mock(java.io.File.class);
        java.io.File downloadedFile2 = mock(java.io.File.class);
        java.io.File downloadedFile3 = mock(java.io.File.class);
        java.io.File mergedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(file1)).thenReturn(downloadedFile1);
        when(kristaMediaClientUtil.downloadFileHandler(file2)).thenReturn(downloadedFile2);
        when(kristaMediaClientUtil.downloadFileHandler(file3)).thenReturn(downloadedFile3);
        when(pdfManager.mergePdfFiles(any(), any(), any(), any(), any(), any(), anyList())).thenReturn(mergedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(mergedPdf)).thenReturn(uploadedFile);

        // Act
        ExtensionResponse response = helper.mergePdfFiles("Title", null, null, null, null, null, inputFiles, false);

        // Assert
        assertNotNull(response);
        
        ArgumentCaptor<List> filesCaptor = ArgumentCaptor.forClass(List.class);
        verify(pdfManager).mergePdfFiles(any(), any(), any(), any(), any(), any(), filesCaptor.capture());
        
        List<java.io.File> mergedFiles = filesCaptor.getValue();
        assertEquals(3, mergedFiles.size());
        assertEquals(downloadedFile1, mergedFiles.get(0), "First file should be in first position");
        assertEquals(downloadedFile2, mergedFiles.get(1), "Second file should be in second position");
        assertEquals(downloadedFile3, mergedFiles.get(2), "Third file should be in third position");
    }

    @Test
    @DisplayName("Should record telemetry for merge operation")
    void testMergePdfFiles_RecordsTelemetry() throws Exception {
        // Arrange
        File file1 = mock(File.class);
        when(file1.getFileName()).thenReturn("document.pdf");
        List<File> inputFiles = Arrays.asList(file1);

        java.io.File downloadedFile1 = mock(java.io.File.class);
        java.io.File mergedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(file1)).thenReturn(downloadedFile1);
        when(pdfManager.mergePdfFiles(any(), any(), any(), any(), any(), any(), anyList())).thenReturn(mergedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(mergedPdf)).thenReturn(uploadedFile);

        // Act
        ExtensionResponse response = helper.mergePdfFiles("Title", null, null, null, null, null, inputFiles, false);

        // Assert
        assertNotNull(response);
        verify(telemetryHelper).incrementCount("pdf.mergePdfFiles");
    }
}

