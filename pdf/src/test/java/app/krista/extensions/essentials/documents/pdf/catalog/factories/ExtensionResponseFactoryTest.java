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

package app.krista.extensions.essentials.documents.pdf.catalog.factories;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.RemediationAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtensionResponseFactoryTest {

    @Test
    @DisplayName("Should create success response with values")
    void testCreate_WithValues() {
        // Arrange
        Map<String, Object> values = new HashMap<>();
        values.put("key1", "value1");
        values.put("key2", 123);

        // Act
        ExtensionResponse response = ExtensionResponseFactory.create(values);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertNull(response.getError());
    }

    @Test
    @DisplayName("Should create failure response with exception and message")
    void testCreate_WithExceptionAndMessage() {
        // Arrange
        Exception cause = new RuntimeException("Test exception");
        String message = "Error occurred";
        ExtensionResponse.Error.ExceptionType exceptionType = ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR;

        // Act
        ExtensionResponse response = ExtensionResponseFactory.create(cause, message, exceptionType);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertNotNull(response.getRemediationActions());
    }

    @Test
    @DisplayName("Should create failure response with exception, actions, and state")
    void testCreate_WithExceptionActionsAndState() {
        // Arrange
        Exception cause = new IllegalArgumentException("Invalid input");
        String message = "Validation failed";
        ExtensionResponse.Error.ExceptionType exceptionType = ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR;
        List<RemediationAction> actions = new ArrayList<>();
        actions.add(RemediationActionFactory.createInformAction("Fix the error", List.of()));
        String subCatalogRequestName = "TestSubCatalog";
        Map<String, Object> state = Map.of("field1", "value1");

        // Act
        ExtensionResponse response = ExtensionResponseFactory.create(
                cause, message, exceptionType, actions, subCatalogRequestName, state);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertNotNull(response.getRemediationActions());
        assertNotNull(response.getState());
    }

    @Test
    @DisplayName("Should create failure response with message, actions, and state (no exception)")
    void testCreate_WithMessageActionsAndState() {
        // Arrange
        String message = "Operation failed";
        ExtensionResponse.Error.ExceptionType exceptionType = ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR;
        List<RemediationAction> actions = new ArrayList<>();
        actions.add(RemediationActionFactory.createInformAction("Retry operation", List.of()));
        String subCatalogRequestName = "RetrySubCatalog";
        Map<String, Object> state = Map.of("retryCount", 1);

        // Act
        ExtensionResponse response = ExtensionResponseFactory.create(
                message, exceptionType, actions, subCatalogRequestName, state);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertNotNull(response.getRemediationActions());
        assertNotNull(response.getState());
    }

    @Test
    @DisplayName("Should create success response with empty values")
    void testCreate_WithEmptyValues() {
        // Arrange
        Map<String, Object> values = new HashMap<>();

        // Act
        ExtensionResponse response = ExtensionResponseFactory.create(values);

        // Assert
        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertNull(response.getError());
    }
}

