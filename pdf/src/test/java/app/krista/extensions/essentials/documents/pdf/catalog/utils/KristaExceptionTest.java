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

package app.krista.extensions.essentials.documents.pdf.catalog.utils;

import app.krista.extension.executor.ExtensionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KristaExceptionTest {

    @Test
    @DisplayName("Should create exception with default message and type")
    void testDefaultConstructor() {
        KristaException exception = new KristaException();

        assertNotNull(exception.getMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, exception.getExceptionType());
    }

    @Test
    @DisplayName("Should create exception with custom message")
    void testConstructorWithMessage() {
        String message = "Custom error message";
        KristaException exception = new KristaException(message);

        assertEquals(message, exception.getMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, exception.getExceptionType());
    }

    @Test
    @DisplayName("Should create exception with message and exception type")
    void testConstructorWithMessageAndType() {
        String message = "Custom error message";
        KristaException exception = new KristaException(message, ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR);

        assertEquals(message, exception.getMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, exception.getExceptionType());
    }

    @Test
    @DisplayName("Should create exception with message, cause, and exception type")
    void testConstructorWithMessageCauseAndType() {
        String message = "Custom error message";
        Throwable cause = new RuntimeException("Root cause");
        KristaException exception = new KristaException(message, cause, ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, exception.getExceptionType());
    }

    @Test
    @DisplayName("Should return correct exception type")
    void testGetExceptionType() {
        KristaException exception = new KristaException("Test", ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR);

        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, exception.getExceptionType());
    }

    @Test
    @DisplayName("Should be throwable")
    void testThrowable() {
        assertThrows(KristaException.class, () -> {
            throw new KristaException("Test exception");
        });
    }

    @Test
    @DisplayName("Should preserve stack trace")
    void testStackTrace() {
        KristaException exception = new KristaException("Test exception");

        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
    }
}

