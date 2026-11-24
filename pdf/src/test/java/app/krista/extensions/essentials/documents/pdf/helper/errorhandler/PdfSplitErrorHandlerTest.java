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

package app.krista.extensions.essentials.documents.pdf.helper.errorhandler;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PdfSplitErrorHandler.
 * Covers every exception classification branch (IOException - corrupted vs. non-corrupted,
 * IllegalArgumentException, and any other unexpected exception) across both the
 * direct-construction (useBuilder=false) and ExtensionResponseBuilder (useBuilder=true) paths.
 */
@DisplayName("PdfSplitErrorHandler Unit Tests")
class PdfSplitErrorHandlerTest {

    private PdfSplitErrorHandler errorHandler;
    private File inputFile;

    @BeforeEach
    void setUp() {
        errorHandler = new PdfSplitErrorHandler();
        inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");
    }

    // ==================== IOException - corrupted PDF branches (useBuilder=false) ====================

    @Test
    @DisplayName("Should classify IOException containing 'Missing root object specification' as corrupted PDF")
    void testHandleException_IOException_MissingRootObject_IsCorrupted() {
        IOException exception = new IOException("Missing root object specification found in document");

        ExtensionResponse response = errorHandler.handleException(exception, inputFile, 3.0, false);

        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().toLowerCase().contains("corrupted"));
    }

    @Test
    @DisplayName("Should classify IOException containing 'Header doesn't contain versioninfo' as corrupted PDF")
    void testHandleException_IOException_HeaderVersionInfo_IsCorrupted() {
        IOException exception = new IOException("Header doesn't contain versioninfo");

        ExtensionResponse response = errorHandler.handleException(exception, inputFile, 3.0, false);

        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().toLowerCase().contains("corrupted"));
    }

    @Test
    @DisplayName("Should classify IOException containing 'Error: Header doesn't contain versioninfo' as corrupted PDF")
    void testHandleException_IOException_ErrorPrefixedHeaderVersionInfo_IsCorrupted() {
        IOException exception = new IOException("Error: Header doesn't contain versioninfo");

        ExtensionResponse response = errorHandler.handleException(exception, inputFile, 3.0, false);

        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().toLowerCase().contains("corrupted"));
    }

    // ==================== IOException - non-corruption branch (useBuilder=false) ====================

    @Test
    @DisplayName("Should classify a non-corruption IOException as a system error with the formatted message")
    void testHandleException_IOException_NonCorruption_IsSystemError() {
        IOException exception = new IOException("Disk is full");

        ExtensionResponse response = errorHandler.handleException(exception, inputFile, 3.0, false);

        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().contains("Disk is full"));
        assertFalse(response.getError().getErrorMessage().toLowerCase().contains("corrupted"));
    }

    @Test
    @DisplayName("Should handle an IOException with a null message and a null input file")
    void testHandleException_IOException_NullMessageAndNullFile() {
        IOException exception = new IOException();

        ExtensionResponse response = errorHandler.handleException(exception, null, null, false);

        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().contains("null"));
    }

    // ==================== IllegalArgumentException branch ====================

    @Test
    @DisplayName("Should classify IllegalArgumentException as an input error carrying the raw message (useBuilder=false)")
    void testHandleException_IllegalArgumentException_IsInputError() {
        IllegalArgumentException exception = new IllegalArgumentException("Bad page number");

        ExtensionResponse response = errorHandler.handleException(exception, inputFile, -1.0, false);

        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, response.getError().getExceptionType());
        assertEquals("Bad page number", response.getError().getErrorMessage());
    }

    // ==================== Unexpected exception branch ====================

    @Test
    @DisplayName("Should classify any other exception as an unexpected system error (useBuilder=false)")
    void testHandleException_UnexpectedException_IsSystemError() {
        RuntimeException exception = new RuntimeException("Something broke");

        ExtensionResponse response = errorHandler.handleException(exception, inputFile, 3.0, false);

        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().contains("Something broke"));
    }

    // ==================== useBuilder=true branches (ExtensionResponseBuilder / ErrorBuilder) ====================

    @Test
    @DisplayName("Should build the response via ExtensionResponseBuilder for a corrupted PDF (useBuilder=true)")
    void testHandleException_UseBuilderTrue_CorruptedPdf() {
        IOException exception = new IOException("Missing root object specification found");

        ExtensionResponse response = errorHandler.handleException(exception, inputFile, 3.0, true);

        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().toLowerCase().contains("corrupted"));
    }

    @Test
    @DisplayName("Should build the response via ExtensionResponseBuilder for an unexpected exception (useBuilder=true)")
    void testHandleException_UseBuilderTrue_UnexpectedException() {
        NullPointerException exception = new NullPointerException("npe occurred");

        ExtensionResponse response = errorHandler.handleException(exception, inputFile, 3.0, true);

        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().contains("npe occurred"));
    }

    @Test
    @DisplayName("Should build the response via ExtensionResponseBuilder for an IllegalArgumentException (useBuilder=true)")
    void testHandleException_UseBuilderTrue_IllegalArgument() {
        IllegalArgumentException exception = new IllegalArgumentException("invalid page");

        ExtensionResponse response = errorHandler.handleException(exception, inputFile, 3.0, true);

        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, response.getError().getExceptionType());
        assertEquals("invalid page", response.getError().getErrorMessage());
    }
}
