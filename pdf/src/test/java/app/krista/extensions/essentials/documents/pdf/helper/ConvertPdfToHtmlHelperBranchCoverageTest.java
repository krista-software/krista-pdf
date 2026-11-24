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
import app.krista.extensions.essentials.documents.pdf.service.PDF2DOMConverter;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * Branch-coverage focused unit tests for {@link ConvertPdfToHtmlHelper}.
 *
 * <p>The helper constructs its own {@link PDF2DOMConverter} instance internally
 * (it is not dependency-injected), so real PDF rendering is intercepted using
 * Mockito's {@code mockConstruction} support (mockito-inline) rather than mocking
 * a constructor-injected collaborator. A handful of branches are only reachable
 * from inside a private helper method that the public method can never invoke
 * with the "other side" of the condition (e.g. the same null-check that already
 * guards the call site) - those are exercised directly via reflection, which is
 * the only way to drive that code path at all.</p>
 */
@DisplayName("ConvertPdfToHtmlHelper Branch Coverage Tests")
class ConvertPdfToHtmlHelperBranchCoverageTest {

    private KristaMediaClientUtil kristaMediaClientUtil;
    private TelemetryHelper telemetryHelper;
    private ConvertPdfToHtmlHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        kristaMediaClientUtil = mock(KristaMediaClientUtil.class);
        telemetryHelper = mock(TelemetryHelper.class);
        // NOTE: the helper's internal PDF2DOMConverter is instantiated eagerly inside its
        // constructor ("new PDF2DOMConverter()"), so for any test that needs that specific
        // instance mocked via Mockito's mockConstruction(), the helper MUST be constructed
        // *inside* the try-with-resources block, after mockConstruction() is opened.
        // For tests that never reach the converter (null-file / early-exception / reflection
        // tests), it is safe to build the helper here with a real (unused) converter.
        helper = new ConvertPdfToHtmlHelper(kristaMediaClientUtil, telemetryHelper);
    }

    // ==================== validatePdfFile() null-guard branch ====================

    @Test
    @DisplayName("Null PDF file should return an input validation error")
    void testConvertPdfToHtml_NullFile_ReturnsValidationError() {
        ExtensionResponse response = helper.convertPdfToHtml(null, true, false, false);

        assertNotNull(response);
        verify(telemetryHelper).incrementCount("pdf.convertPdfToHtml");
        verify(telemetryHelper).recordError(eq("pdf.convertPdfToHtml"), anyLong(), any(IllegalArgumentException.class), any());
        verifyNoInteractions(kristaMediaClientUtil);
    }

    // ==================== Success paths: saveAsTxt true/false branches ====================

    @Test
    @DisplayName("Non-null PDF with saveAsTextFile=false should save as .html and clean up the temp file")
    void testConvertPdfToHtml_Success_SaveAsHtml() throws Exception {
        File pdfFile = mock(File.class);
        when(pdfFile.getFileName()).thenReturn("sample.pdf");
        java.io.File downloadedPdf = mock(java.io.File.class);
        File uploadedHtml = mock(File.class);

        when(kristaMediaClientUtil.downloadFileHandler(pdfFile)).thenReturn(downloadedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(any(java.io.File.class))).thenReturn(uploadedHtml);

        try (MockedConstruction<PDF2DOMConverter> mocked = mockConstruction(PDF2DOMConverter.class,
                (mock, context) -> when(mock.convertPdfToHtml(any(java.io.File.class), anyBoolean()))
                        .thenReturn("<html><body>Hello</body></html>"))) {

            // Construct the helper only now, while mockConstruction() is active, so that its
            // internally-created PDF2DOMConverter instance is the mocked one.
            ConvertPdfToHtmlHelper localHelper = new ConvertPdfToHtmlHelper(kristaMediaClientUtil, telemetryHelper);
            ExtensionResponse response = localHelper.convertPdfToHtml(pdfFile, true, false, false);

            assertNotNull(response);
            assertEquals(1, mocked.constructed().size());
            verify(kristaMediaClientUtil).downloadFileHandler(pdfFile);

            // The real temp html file passed to uploadFileHandler must have a .html extension
            // and must no longer exist afterward - proves cleanupTemporaryFile() deleted it
            // (tempFile != null && tempFile.exists() -> true branch, delete() succeeded -> !deleted -> false branch).
            org.mockito.ArgumentCaptor<java.io.File> captor = org.mockito.ArgumentCaptor.forClass(java.io.File.class);
            verify(kristaMediaClientUtil).uploadFileHandler(captor.capture());
            java.io.File uploadedTempFile = captor.getValue();
            assertTrue(uploadedTempFile.getName().endsWith(".html"));
            assertFalse(uploadedTempFile.exists(), "temp html file should have been cleaned up");

            verify(telemetryHelper).recordSuccess(eq("pdf.convertPdfToHtml"), anyLong(), any());
        }
    }

    @Test
    @DisplayName("Non-null PDF with saveAsTextFile=true should save as _html_source.txt")
    void testConvertPdfToHtml_Success_SaveAsText() throws Exception {
        File pdfFile = mock(File.class);
        when(pdfFile.getFileName()).thenReturn("sample.PDF");
        java.io.File downloadedPdf = mock(java.io.File.class);
        File uploadedHtml = mock(File.class);

        when(kristaMediaClientUtil.downloadFileHandler(pdfFile)).thenReturn(downloadedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(any(java.io.File.class))).thenReturn(uploadedHtml);

        try (MockedConstruction<PDF2DOMConverter> mocked = mockConstruction(PDF2DOMConverter.class,
                (mock, context) -> when(mock.convertPdfToHtml(any(java.io.File.class), anyBoolean()))
                        .thenReturn("plain text source"))) {

            ConvertPdfToHtmlHelper localHelper = new ConvertPdfToHtmlHelper(kristaMediaClientUtil, telemetryHelper);
            ExtensionResponse response = localHelper.convertPdfToHtml(pdfFile, null, true, true);

            assertNotNull(response);
            org.mockito.ArgumentCaptor<java.io.File> captor = org.mockito.ArgumentCaptor.forClass(java.io.File.class);
            verify(kristaMediaClientUtil).uploadFileHandler(captor.capture());
            assertTrue(captor.getValue().getName().endsWith("_html_source.txt"));

            verify(telemetryHelper).recordSuccess(eq("pdf.convertPdfToHtml"), anyLong(), any());
        }
    }

    // ==================== catch(IllegalArgumentException) with non-null file branch ====================

    @Test
    @DisplayName("Non-null PDF whose download raises IllegalArgumentException should hit the input-error branch (not the null branch)")
    void testConvertPdfToHtml_DownloadThrowsIllegalArgumentException_NonNullFile() {
        File pdfFile = mock(File.class);
        when(pdfFile.getFileName()).thenReturn("broken.pdf");

        when(kristaMediaClientUtil.downloadFileHandler(pdfFile))
                .thenThrow(new IllegalArgumentException("Corrupt PDF"));

        ExtensionResponse response = helper.convertPdfToHtml(pdfFile, false, false, false);

        assertNotNull(response);
        verify(telemetryHelper).recordError(eq("pdf.convertPdfToHtml"), anyLong(), any(IllegalArgumentException.class), any());
    }

    // ==================== convertToHtmlContent() null-content branch -> catch(Exception) ====================

    @Test
    @DisplayName("Converter returning null content should blow up while writing and hit the generic SYSTEM_ERROR branch")
    void testConvertPdfToHtml_NullContentFromConverter_ReturnsSystemError() {
        File pdfFile = mock(File.class);
        when(pdfFile.getFileName()).thenReturn("empty.pdf");
        java.io.File downloadedPdf = mock(java.io.File.class);

        when(kristaMediaClientUtil.downloadFileHandler(pdfFile)).thenReturn(downloadedPdf);

        try (MockedConstruction<PDF2DOMConverter> mocked = mockConstruction(PDF2DOMConverter.class,
                (mock, context) -> {
                    try {
                        when(mock.convertPdfToHtml(any(java.io.File.class), anyBoolean())).thenReturn(null);
                    } catch (Exception ignored) {
                        // convertPdfToHtml declares IOException; the stub above never actually throws it
                    }
                })) {

            ConvertPdfToHtmlHelper localHelper = new ConvertPdfToHtmlHelper(kristaMediaClientUtil, telemetryHelper);
            ExtensionResponse response = localHelper.convertPdfToHtml(pdfFile, true, false, false);

            assertNotNull(response);
            // htmlContent == null causes a NullPointerException inside FileWriter.write(),
            // which is caught by the generic catch(Exception) -> handleUnexpectedError(..., pdfFile) branch,
            // with pdfFile != null (true branch of that method's ternary).
            verify(telemetryHelper).recordError(eq("pdf.convertPdfToHtml"), anyLong(), any(Exception.class), any());
            verify(kristaMediaClientUtil, never()).uploadFileHandler(any());
        }
    }

    // ==================== cleanupTemporaryFile(): tempFile.exists() == false branch ====================

    @Test
    @DisplayName("If the temp file is already gone by cleanup time, delete() must not be attempted")
    void testConvertPdfToHtml_TempFileAlreadyDeleted_SkipsDeleteCall() throws Exception {
        File pdfFile = mock(File.class);
        when(pdfFile.getFileName()).thenReturn("vanish.pdf");
        java.io.File downloadedPdf = mock(java.io.File.class);
        File uploadedHtml = mock(File.class);

        when(kristaMediaClientUtil.downloadFileHandler(pdfFile)).thenReturn(downloadedPdf);
        // Side effect: delete the real temp html file as soon as it is "uploaded",
        // so that by the time cleanupTemporaryFile() runs, tempFile.exists() == false.
        when(kristaMediaClientUtil.uploadFileHandler(any(java.io.File.class))).thenAnswer(invocation -> {
            java.io.File tempFile = invocation.getArgument(0);
            tempFile.delete();
            return uploadedHtml;
        });

        try (MockedConstruction<PDF2DOMConverter> mocked = mockConstruction(PDF2DOMConverter.class,
                (mock, context) -> when(mock.convertPdfToHtml(any(java.io.File.class), anyBoolean()))
                        .thenReturn("<html></html>"))) {

            ConvertPdfToHtmlHelper localHelper = new ConvertPdfToHtmlHelper(kristaMediaClientUtil, telemetryHelper);
            ExtensionResponse response = localHelper.convertPdfToHtml(pdfFile, false, false, false);

            assertNotNull(response);
            verify(telemetryHelper).recordSuccess(eq("pdf.convertPdfToHtml"), anyLong(), any());
        }
    }

    // ==================== cleanupTemporaryFile(): delete() returns false -> if(!deleted) true branch ====================

    @Test
    @DisplayName("cleanupTemporaryFile(): a non-deletable (non-empty directory) path hits the delete-failed warning branch")
    void testCleanupTemporaryFile_DeleteFails_ViaReflection() throws Exception {
        Path dir = Files.createTempDirectory("cleanup-branch-test");
        Path child = dir.resolve("child.txt");
        Files.write(child, "blocking".getBytes());
        java.io.File nonEmptyDir = dir.toFile();

        assertTrue(nonEmptyDir.exists());

        Method cleanup = ConvertPdfToHtmlHelper.class.getDeclaredMethod("cleanupTemporaryFile", java.io.File.class);
        cleanup.setAccessible(true);
        // A non-empty directory exists() == true but delete() returns false on every mainstream OS.
        cleanup.invoke(helper, (Object) nonEmptyDir);

        assertTrue(nonEmptyDir.exists(), "directory deletion should have failed and been left in place");

        // cleanup
        Files.deleteIfExists(child);
        Files.deleteIfExists(dir);
    }

    // ==================== cleanupTemporaryFile(): tempFile == null branch ====================

    @Test
    @DisplayName("cleanupTemporaryFile(): a null file is a safe no-op")
    void testCleanupTemporaryFile_NullFile_ViaReflection() throws Exception {
        Method cleanup = ConvertPdfToHtmlHelper.class.getDeclaredMethod("cleanupTemporaryFile", java.io.File.class);
        cleanup.setAccessible(true);
        assertDoesNotThrow(() -> cleanup.invoke(helper, (Object) null));
    }

    // ==================== handleUnexpectedError(): pdfFile == null branch (unreachable via public API) ====================

    @Test
    @DisplayName("handleUnexpectedError(): null pdfFile takes the N/A ternary branch")
    void testHandleUnexpectedError_NullPdfFile_ViaReflection() throws Exception {
        Method handleUnexpectedError = ConvertPdfToHtmlHelper.class.getDeclaredMethod(
                "handleUnexpectedError", Exception.class, long.class, File.class);
        handleUnexpectedError.setAccessible(true);

        ExtensionResponse response = (ExtensionResponse) handleUnexpectedError.invoke(
                helper, new RuntimeException("boom"), System.currentTimeMillis(), null);

        assertNotNull(response);
        verify(telemetryHelper).recordError(eq("pdf.convertPdfToHtml"), anyLong(), any(Exception.class), any());
    }

    // ==================== recordSuccessTelemetry(): htmlContent == null branch (unreachable via public API) ====================

    @Test
    @DisplayName("recordSuccessTelemetry(): null htmlContent takes the content_length=0 ternary branch")
    void testRecordSuccessTelemetry_NullContent_ViaReflection() throws Exception {
        File pdfFile = mock(File.class);
        when(pdfFile.getFileName()).thenReturn("doc.pdf");
        File htmlFile = mock(File.class);
        when(htmlFile.getFileName()).thenReturn("doc.html");

        Method recordSuccessTelemetry = ConvertPdfToHtmlHelper.class.getDeclaredMethod(
                "recordSuccessTelemetry", long.class, File.class, boolean.class, boolean.class, String.class, File.class);
        recordSuccessTelemetry.setAccessible(true);

        recordSuccessTelemetry.invoke(helper, System.currentTimeMillis(), pdfFile, true, false, null, htmlFile);

        verify(telemetryHelper).recordSuccess(eq("pdf.convertPdfToHtml"), anyLong(), any());
    }
}
