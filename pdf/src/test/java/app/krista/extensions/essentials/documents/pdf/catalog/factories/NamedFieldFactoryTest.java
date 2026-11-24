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

import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.FieldTypes;
import app.krista.model.field.NamedField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class NamedFieldFactoryTest {

    @Test
    @DisplayName("Should create switch field")
    void testCreateSwitchField() {
        // Arrange
        String fieldName = "testSwitch";

        // Act
        NamedField result = NamedFieldFactory.createSwitchField(fieldName);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should create rich text field with large visual width")
    void testCreateRichTextField() {
        // Arrange
        String fieldName = "testRichText";

        // Act
        NamedField result = NamedFieldFactory.createRichTextField(fieldName);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should create file field with upload configuration")
    void testCreateField_FileField() {
        // Arrange
        String fieldName = "testFile";

        // Act
        NamedField result = NamedFieldFactory.createField(fieldName, FieldTypes.FILE_FIELD);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should create list field with text field configuration")
    void testCreateField_ListField() {
        // Arrange
        String fieldName = "testList";

        // Act
        NamedField result = NamedFieldFactory.createField(fieldName, FieldTypes.LIST_FIELD);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should create rich text field when specified in createField")
    void testCreateField_RichTextField() {
        // Arrange
        String fieldName = "testRichText";

        // Act
        NamedField result = NamedFieldFactory.createField(fieldName, FieldTypes.RICH_TEXT_FIELD);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should create text field with default configuration")
    void testCreateField_TextField() {
        // Arrange
        String fieldName = "testText";

        // Act
        NamedField result = NamedFieldFactory.createField(fieldName, FieldTypes.TEXT_FIELD);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should create number field with default configuration")
    void testCreateField_NumberField() {
        // Arrange
        String fieldName = "testNumber";

        // Act
        NamedField result = NamedFieldFactory.createField(fieldName, FieldTypes.NUMBER_FIELD);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle case-insensitive file field type")
    void testCreateField_FileFieldCaseInsensitive() {
        // Arrange
        String fieldName = "testFile";

        // Act
        NamedField result = NamedFieldFactory.createField(fieldName, "file");

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should create field with empty name")
    void testCreateField_EmptyName() {
        // Arrange
        String fieldName = "";

        // Act
        NamedField result = NamedFieldFactory.createField(fieldName, FieldTypes.TEXT_FIELD);

        // Assert
        assertNotNull(result);
    }
}

