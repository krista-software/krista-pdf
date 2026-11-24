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

package app.krista.extensions.essentials.documents.pdf.util;

import app.krista.extensions.essentials.documents.pdf.client.KristaMediaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KristaMediaClientUtil.
 * Verifies that download/upload delegate correctly to the injected KristaMediaClient,
 * including the null short-circuit on download and the RuntimeException wrapping of IOException.
 */
@DisplayName("KristaMediaClientUtil Unit Tests")
class KristaMediaClientUtilTest {

    @Mock
    private KristaMediaClient kristaMediaClient;

    @Mock
    private app.krista.model.base.File kristaFile;

    @Mock
    private java.io.File javaFile;

    private KristaMediaClientUtil kristaMediaClientUtil;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        kristaMediaClientUtil = new KristaMediaClientUtil(kristaMediaClient);
    }

    // ==================== downloadFileHandler() Tests ====================

    @Test
    @DisplayName("Should return null without calling the client when the input file is null")
    void testDownloadFileHandler_NullFile_ReturnsNull() {
        java.io.File result = kristaMediaClientUtil.downloadFileHandler(null);

        assertNull(result);
        verifyNoInteractions(kristaMediaClient);
    }

    @Test
    @DisplayName("Should delegate to KristaMediaClient.toJavaFile for a non-null file")
    void testDownloadFileHandler_ValidFile_DelegatesToClient() throws IOException {
        when(kristaMediaClient.toJavaFile(kristaFile)).thenReturn(javaFile);

        java.io.File result = kristaMediaClientUtil.downloadFileHandler(kristaFile);

        assertSame(javaFile, result);
        verify(kristaMediaClient).toJavaFile(kristaFile);
    }

    @Test
    @DisplayName("Should wrap an IOException from the client in a RuntimeException")
    void testDownloadFileHandler_IOExceptionFromClient_ThrowsRuntimeException() throws IOException {
        IOException cause = new IOException("download failed");
        when(kristaMediaClient.toJavaFile(kristaFile)).thenThrow(cause);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> kristaMediaClientUtil.downloadFileHandler(kristaFile));

        assertSame(cause, thrown.getCause());
        verify(kristaMediaClient).toJavaFile(kristaFile);
    }

    // ==================== uploadFileHandler() Tests ====================

    @Test
    @DisplayName("Should delegate to KristaMediaClient.toKristaFile for a Java file")
    void testUploadFileHandler_ValidFile_DelegatesToClient() throws IOException {
        when(kristaMediaClient.toKristaFile(javaFile)).thenReturn(kristaFile);

        app.krista.model.base.File result = kristaMediaClientUtil.uploadFileHandler(javaFile);

        assertSame(kristaFile, result);
        verify(kristaMediaClient).toKristaFile(javaFile);
    }

    @Test
    @DisplayName("Should wrap an IOException from the client in a RuntimeException on upload")
    void testUploadFileHandler_IOExceptionFromClient_ThrowsRuntimeException() throws IOException {
        IOException cause = new IOException("upload failed");
        when(kristaMediaClient.toKristaFile(javaFile)).thenThrow(cause);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> kristaMediaClientUtil.uploadFileHandler(javaFile));

        assertSame(cause, thrown.getCause());
        verify(kristaMediaClient).toKristaFile(javaFile);
    }
}
