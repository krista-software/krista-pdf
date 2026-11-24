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

import app.krista.extension.executor.RemediationAction;
import app.krista.model.field.NamedField;
import app.krista.model.field.NamedValuedField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RemediationActionFactoryTest {

    @Test
    @DisplayName("Should create ask action with message and fields")
    void testCreateAskAction() {
        // Arrange
        String message = "Please provide input";
        List<NamedField> fields = new ArrayList<>();
        fields.add(new NamedField("field1", "text", null, null));

        // Act
        RemediationAction result = RemediationActionFactory.createAskAction(message, fields);

        // Assert
        assertNotNull(result);
        assertEquals(RemediationAction.RecipientType.ACTIVE_USER, result.getRecipientType());
    }

    @Test
    @DisplayName("Should create ask action with empty fields")
    void testCreateAskAction_EmptyFields() {
        // Arrange
        String message = "Confirm action";
        List<NamedField> fields = new ArrayList<>();

        // Act
        RemediationAction result = RemediationActionFactory.createAskAction(message, fields);

        // Assert
        assertNotNull(result);
        assertEquals(RemediationAction.RecipientType.ACTIVE_USER, result.getRecipientType());
    }

    @Test
    @DisplayName("Should create inform action with message and fields")
    void testCreateInformAction() {
        // Arrange
        String message = "Operation completed";
        List<NamedValuedField> fields = new ArrayList<>();
        fields.add(new NamedValuedField("result", "text", "success", null, null));

        // Act
        RemediationAction result = RemediationActionFactory.createInformAction(message, fields);

        // Assert
        assertNotNull(result);
        assertEquals(RemediationAction.RecipientType.ACTIVE_USER, result.getRecipientType());
    }

    @Test
    @DisplayName("Should create inform action with empty fields")
    void testCreateInformAction_EmptyFields() {
        // Arrange
        String message = "Process started";
        List<NamedValuedField> fields = new ArrayList<>();

        // Act
        RemediationAction result = RemediationActionFactory.createInformAction(message, fields);

        // Assert
        assertNotNull(result);
        assertEquals(RemediationAction.RecipientType.ACTIVE_USER, result.getRecipientType());
    }

    @Test
    @DisplayName("Should create inform action for all participants")
    void testCreateInformActionALLParticipants() {
        // Arrange
        String message = "Broadcast message";
        List<NamedValuedField> fields = new ArrayList<>();
        fields.add(new NamedValuedField("status", "text", "completed", null, null));

        // Act
        RemediationAction result = RemediationActionFactory.createInformActionALLParticipants(message, fields);

        // Assert
        assertNotNull(result);
        assertEquals(RemediationAction.RecipientType.ALL_PARTICIPANTS, result.getRecipientType());
    }

    @Test
    @DisplayName("Should create inform action for all participants with empty fields")
    void testCreateInformActionALLParticipants_EmptyFields() {
        // Arrange
        String message = "System notification";
        List<NamedValuedField> fields = new ArrayList<>();

        // Act
        RemediationAction result = RemediationActionFactory.createInformActionALLParticipants(message, fields);

        // Assert
        assertNotNull(result);
        assertEquals(RemediationAction.RecipientType.ALL_PARTICIPANTS, result.getRecipientType());
    }

    @Test
    @DisplayName("Should create ask action with multiple fields")
    void testCreateAskAction_MultipleFields() {
        // Arrange
        String message = "Enter details";
        List<NamedField> fields = new ArrayList<>();
        fields.add(new NamedField("name", "text", null, null));
        fields.add(new NamedField("age", "number", null, null));
        fields.add(new NamedField("email", "text", null, null));

        // Act
        RemediationAction result = RemediationActionFactory.createAskAction(message, fields);

        // Assert
        assertNotNull(result);
        assertEquals(RemediationAction.RecipientType.ACTIVE_USER, result.getRecipientType());
    }

    @Test
    @DisplayName("Should create inform action with multiple fields")
    void testCreateInformAction_MultipleFields() {
        // Arrange
        String message = "Results";
        List<NamedValuedField> fields = new ArrayList<>();
        fields.add(new NamedValuedField("count", "number", 10, null, null));
        fields.add(new NamedValuedField("status", "text", "success", null, null));
        fields.add(new NamedValuedField("timestamp", "text", "2024-01-01", null, null));

        // Act
        RemediationAction result = RemediationActionFactory.createInformAction(message, fields);

        // Assert
        assertNotNull(result);
        assertEquals(RemediationAction.RecipientType.ACTIVE_USER, result.getRecipientType());
    }
}

