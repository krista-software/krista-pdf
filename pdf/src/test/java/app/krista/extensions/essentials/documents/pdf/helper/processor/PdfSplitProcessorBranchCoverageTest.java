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

package app.krista.extensions.essentials.documents.pdf.helper.processor;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extensions.essentials.documents.pdf.service.PDFManager;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Branch-coverage tests for {@link PdfSplitProcessor}, which had 0% branch coverage (14
 * missed branches, no prior test class) before this suite.
 *
 * <p>{@code execute(File, int)} branches targeted:</p>
 * <ul>
 *   <li>{@code javaFiles == null} (short-circuit true) via a mocked {@code null} return.</li>
 *   <li>{@code javaFiles == null} false, {@code javaFiles.isEmpty()} true, via an empty list.</li>
 *   <li>{@code javaFiles == null} false, {@code javaFiles.isEmpty()} false (normal success
 *       path with 2 files), which also drives both branches of the {@code for} loop over
 *       {@code javaFiles} (enter the body at least once, then exit when exhausted).</li>
 * </ul>
 *
 * <p>{@code buildSuccessResponse(List)} branches targeted:</p>
 * <ul>
 *   <li>{@code splitFiles == null} true -- fallback response built from an empty list.</li>
 *   <li>{@code splitFiles == null} false, {@code splitFiles.size() != 2} true (e.g. 1 or 3
 *       files) -- fallback response built from the original (non-empty) list.</li>
 *   <li>{@code splitFiles == null} false, {@code splitFiles.size() != 2} false (exactly 2
 *       files) -- the "First Segment" / "Second Segment" success path.</li>
 * </ul>
 *
 * <p>The null-list and wrong-size-list fallback cases together exercise both outcomes of the
 * {@code splitFiles != null ? splitFiles.size() : "null"} and
 * {@code splitFiles != null ? splitFiles : new ArrayList<>()} ternaries used to build the
 * warning log and the fallback response map.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PdfSplitProcessor Branch Coverage Tests")
public class PdfSplitProcessorBranchCoverageTest {

    @Mock
    private PDFManager pdfManager;

    @Mock
    private KristaMediaClientUtil kristaMediaClientUtil;

    private PdfSplitProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new PdfSplitProcessor(pdfManager, kristaMediaClientUtil);
    }

    // ==================== execute(): javaFiles == null (short-circuit true) ====================

    @Test
    @DisplayName("execute() should return success with an empty list when the manager returns a hard null")
    void testExecute_SplitReturnsNull_ReturnsSuccessWithEmptyList() throws Exception {
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(pdfManager.splitPDFAtPageNumber(downloadedFile, 3)).thenReturn(null);

        PdfSplitProcessor.ProcessResult result = processor.execute(inputFile, 3);

        assertTrue(result.isSuccess());
        assertNotNull(result.getSplitFiles());
        assertTrue(result.getSplitFiles().isEmpty());
        assertNull(result.getException());
        verify(kristaMediaClientUtil, never()).uploadFileHandler(any());
    }

    // ==================== execute(): javaFiles non-null but empty ====================

    @Test
    @DisplayName("execute() should return success with an empty list when the manager returns an empty list")
    void testExecute_SplitReturnsEmptyList_ReturnsSuccessWithEmptyList() throws Exception {
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(pdfManager.splitPDFAtPageNumber(downloadedFile, 3)).thenReturn(new ArrayList<>());

        PdfSplitProcessor.ProcessResult result = processor.execute(inputFile, 3);

        assertTrue(result.isSuccess());
        assertTrue(result.getSplitFiles().isEmpty());
        verify(kristaMediaClientUtil, never()).uploadFileHandler(any());
    }

    // ==================== execute(): success path, also exercises both for-loop branches ====================

    @Test
    @DisplayName("execute() should download, split, and upload every file on the happy path")
    void testExecute_SuccessfulSplit_UploadsEveryFileInOrder() throws Exception {
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);
        java.io.File splitJavaFile1 = mock(java.io.File.class);
        java.io.File splitJavaFile2 = mock(java.io.File.class);
        List<java.io.File> javaFiles = Arrays.asList(splitJavaFile1, splitJavaFile2);

        File uploaded1 = mock(File.class);
        File uploaded2 = mock(File.class);

        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(pdfManager.splitPDFAtPageNumber(downloadedFile, 5)).thenReturn(javaFiles);
        when(kristaMediaClientUtil.uploadFileHandler(splitJavaFile1)).thenReturn(uploaded1);
        when(kristaMediaClientUtil.uploadFileHandler(splitJavaFile2)).thenReturn(uploaded2);

        PdfSplitProcessor.ProcessResult result = processor.execute(inputFile, 5);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getSplitFiles().size());
        assertEquals(uploaded1, result.getSplitFiles().get(0));
        assertEquals(uploaded2, result.getSplitFiles().get(1));
        verify(kristaMediaClientUtil).uploadFileHandler(splitJavaFile1);
        verify(kristaMediaClientUtil).uploadFileHandler(splitJavaFile2);
    }

    // ==================== execute(): exception paths (line coverage safety net) ====================

    @Test
    @DisplayName("execute() should wrap an IOException from the manager into a failure ProcessResult")
    void testExecute_IOExceptionDuringSplit_ReturnsFailure() throws Exception {
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        IOException ioException = new IOException("boom");
        when(pdfManager.splitPDFAtPageNumber(downloadedFile, 2)).thenThrow(ioException);

        PdfSplitProcessor.ProcessResult result = processor.execute(inputFile, 2);

        assertFalse(result.isSuccess());
        assertNull(result.getSplitFiles());
        assertEquals(ioException, result.getException());
    }

    @Test
    @DisplayName("execute() should wrap an unexpected RuntimeException into a failure ProcessResult")
    void testExecute_UnexpectedException_ReturnsFailure() throws Exception {
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        RuntimeException unexpected = new RuntimeException("download blew up");
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenThrow(unexpected);

        PdfSplitProcessor.ProcessResult result = processor.execute(inputFile, 2);

        assertFalse(result.isSuccess());
        assertEquals(unexpected, result.getException());
    }

    // ==================== buildSuccessResponse(): splitFiles == null ====================

    @Test
    @DisplayName("buildSuccessResponse() should fall back to an empty 'Split Files' list when splitFiles is null")
    void testBuildSuccessResponse_NullList_FallsBackToEmptyList() {
        ExtensionResponse response = processor.buildSuccessResponse(null);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertEquals(new ArrayList<>(), response.getResponseValue().get("Split Files"));
        assertFalse(response.getResponseValue().containsKey("First Segment"));
    }

    // ==================== buildSuccessResponse(): splitFiles non-null, size != 2 ====================

    @Test
    @DisplayName("buildSuccessResponse() should fall back to the original list when splitFiles has the wrong size")
    void testBuildSuccessResponse_WrongSizeList_FallsBackToOriginalList() {
        File file1 = mock(File.class);
        File file2 = mock(File.class);
        File file3 = mock(File.class);
        List<File> threeFiles = Arrays.asList(file1, file2, file3);

        ExtensionResponse response = processor.buildSuccessResponse(threeFiles);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertEquals(threeFiles, response.getResponseValue().get("Split Files"));
        assertFalse(response.getResponseValue().containsKey("First Segment"));
    }

    @Test
    @DisplayName("buildSuccessResponse() should fall back to the original single-element list when splitFiles has size 1")
    void testBuildSuccessResponse_SingleFileList_FallsBackToOriginalList() {
        File onlyFile = mock(File.class);
        List<File> oneFile = Collections.singletonList(onlyFile);

        ExtensionResponse response = processor.buildSuccessResponse(oneFile);

        assertNotNull(response);
        assertEquals(oneFile, response.getResponseValue().get("Split Files"));
    }

    // ==================== buildSuccessResponse(): splitFiles non-null, size == 2 ====================

    @Test
    @DisplayName("buildSuccessResponse() should return First/Second Segment keys when splitFiles has exactly 2 files")
    void testBuildSuccessResponse_ExactlyTwoFiles_ReturnsFirstAndSecondSegment() {
        File first = mock(File.class);
        File second = mock(File.class);
        List<File> twoFiles = Arrays.asList(first, second);

        ExtensionResponse response = processor.buildSuccessResponse(twoFiles);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertEquals(first, response.getResponseValue().get("First Segment"));
        assertEquals(second, response.getResponseValue().get("Second Segment"));
        assertFalse(response.getResponseValue().containsKey("Split Files"));
    }
}
