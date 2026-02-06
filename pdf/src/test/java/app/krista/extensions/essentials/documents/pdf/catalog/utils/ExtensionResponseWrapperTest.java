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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtensionResponseWrapperTest {

    @Test
    @DisplayName("Should wrap successful response")
    void testWrapResponse_Success() {
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("key", "value");

        ExtensionResponse response = ExtensionResponseWrapper.wrapResponse(() -> expectedResult);

        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertNull(response.getError());
    }

    @Test
    @DisplayName("Should wrap KristaException as failure response")
    void testWrapResponse_KristaException() {
        String errorMessage = "Krista error occurred";

        ExtensionResponse response = ExtensionResponseWrapper.wrapResponse(() -> {
            throw new KristaException(errorMessage, ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR);
        });

        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, response.getError().getExceptionType());
    }

    @Test
    @DisplayName("Should wrap generic Exception as failure response")
    void testWrapResponse_GenericException() {
        String errorMessage = "Generic error occurred";

        ExtensionResponse response = ExtensionResponseWrapper.wrapResponse(() -> {
            throw new RuntimeException(errorMessage);
        });

        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, response.getError().getExceptionType());
    }

    @Test
    @DisplayName("Should include stack trace in error response")
    void testWrapResponse_IncludesStackTrace() {
        ExtensionResponse response = ExtensionResponseWrapper.wrapResponse(() -> {
            throw new RuntimeException("Test error");
        });

        assertNotNull(response.getError());
        assertNotNull(response.getError().getStackTrace());
        assertTrue(response.getError().getStackTrace().contains("RuntimeException"));
    }

    @Test
    @DisplayName("Should handle null result from supplier")
    void testWrapResponse_NullResult() {
        ExtensionResponse response = ExtensionResponseWrapper.wrapResponse(() -> null);

        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertNull(response.getError());
    }

    @Test
    @DisplayName("Should handle empty map result")
    void testWrapResponse_EmptyMap() {
        Map<String, Object> emptyMap = new HashMap<>();

        ExtensionResponse response = ExtensionResponseWrapper.wrapResponse(() -> emptyMap);

        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertNull(response.getError());
    }
}

