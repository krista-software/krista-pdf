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
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("RenamePdfHelper Unit Tests")
class RenamePdfHelperTest {

    private KristaMediaClientUtil mediaClientUtil;
    private TelemetryHelper telemetryHelper;
    private RenamePdfHelper helper;

    @BeforeEach
    void setUp() {
        mediaClientUtil = mock(KristaMediaClientUtil.class);
        telemetryHelper = mock(TelemetryHelper.class);
        helper = new RenamePdfHelper(mediaClientUtil, telemetryHelper);
    }

    private File pdfInput(String name) {
        File f = mock(File.class);
        when(f.getFileName()).thenReturn(name);
        return f;
    }

    @Test
    @DisplayName("happy path: uploads under sanitized name, preserving case")
    void happyPath() throws IOException {
        Path src = Files.createTempFile("src-", ".pdf");
        Files.write(src, new byte[]{1, 2, 3});
        when(mediaClientUtil.downloadFileHandler(any())).thenReturn(src.toFile());
        File uploaded = mock(File.class);
        when(mediaClientUtil.uploadFileHandler(any())).thenReturn(uploaded);

        ExtensionResponse response = helper.renamePdf(pdfInput("original.pdf"), "Invoice March");

        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertNull(response.getError());
        assertSame(uploaded, response.getResponseValue().get("Renamed PDF File"));

        ArgumentCaptor<java.io.File> captor = ArgumentCaptor.forClass(java.io.File.class);
        verify(mediaClientUtil).uploadFileHandler(captor.capture());
        assertEquals("Invoice March.pdf", captor.getValue().getName());
    }

    @Test
    @DisplayName("non-PDF input fails fast with INPUT_ERROR and no download/upload")
    void nonPdfInput() {
        ExtensionResponse response = helper.renamePdf(pdfInput("notes.txt"), "Invoice");
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                response.getError().getExceptionType());
        verify(mediaClientUtil, never()).downloadFileHandler(any());
        verify(mediaClientUtil, never()).uploadFileHandler(any());
    }

    @Test
    @DisplayName("null input fails fast with INPUT_ERROR")
    void nullInput() {
        ExtensionResponse response = helper.renamePdf(null, "Invoice");
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                response.getError().getExceptionType());
        verify(mediaClientUtil, never()).downloadFileHandler(any());
    }

    @Test
    @DisplayName("blank name fails fast with INPUT_ERROR")
    void blankName() {
        ExtensionResponse response = helper.renamePdf(pdfInput("original.pdf"), "   ");
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                response.getError().getExceptionType());
        verify(mediaClientUtil, never()).downloadFileHandler(any());
    }

    @Test
    @DisplayName("name with only special characters fails fast with INPUT_ERROR")
    void onlySpecialChars() {
        ExtensionResponse response = helper.renamePdf(pdfInput("original.pdf"), "***");
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                response.getError().getExceptionType());
        verify(mediaClientUtil, never()).downloadFileHandler(any());
    }

    @Test
    @DisplayName("I/O failure during download maps to SYSTEM_ERROR")
    void downloadFailure() {
        when(mediaClientUtil.downloadFileHandler(any())).thenThrow(new RuntimeException("boom"));
        ExtensionResponse response = helper.renamePdf(pdfInput("original.pdf"), "Invoice");
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                response.getError().getExceptionType());
    }
}
