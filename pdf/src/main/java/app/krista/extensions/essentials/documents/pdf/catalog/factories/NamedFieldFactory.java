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

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class for creating NamedField objects.
 * <p>
 * This class provides utility methods to create and manipulate NamedField objects
 * used in the Salesforce Customer Service extension. NamedFields are used to represent
 * form fields with different types and configurations.
 * <p>
 * The factory encapsulates the complexity of creating properly formatted NamedField objects
 * with appropriate metadata and ensures consistency across the application.
 */
public class NamedFieldFactory {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private NamedFieldFactory() {
    }

    /**
     * Creates a switch field with the specified name.
     * <p>
     * Switch fields are toggle fields that can be turned on or off.
     *
     * @param fieldName The name of the switch field
     * @return A NamedField object configured as a switch field
     */
    public static NamedField createSwitchField(String fieldName) {
        return new NamedField(fieldName, FieldTypes.SWITCH_FIELD, Map.of(), Map.of());
    }

    /**
     * Creates a rich text field with the specified name.
     *
     * @param fieldName The name of the rich text field
     * @return A NamedField object configured as a rich text field
     */
    public static NamedField createRichTextField(String fieldName) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("visualWidth", "L");  // Large visual width for rich text
        return new NamedField(fieldName, FieldTypes.RICH_TEXT_FIELD, attributes, Map.of());
    }

    /**
     * Creates a field with the specified name and type.
     * <p>
     * This method handles special configuration for different field types:
     * - File fields: Configured with file upload settings
     * - List fields: Configured to hold text fields
     * - Rich text fields: Configured with large visual width
     * - Other fields: Created with default configuration
     *
     * @param fieldName The name of the field
     * @param fieldType The type of the field (from FieldTypes)
     * @return A NamedField object configured according to the specified type
     */
    public static NamedField createField(String fieldName, String fieldType) {
        if (fieldType.equalsIgnoreCase(FieldTypes.FILE_FIELD)) {
            // Create the Map<String, Object> to represent the JSON structure
            Map<String, Object> fileUploadConfig = new HashMap<>();

            fileUploadConfig.put("visualWidth", "S");
            fileUploadConfig.put("multipleFileUpload", true);
            fileUploadConfig.put("minFileSizeUnit", "KB");
            fileUploadConfig.put("maxFileSizeUnit", "MB");
            fileUploadConfig.put("fileTypes", "[\"Document\",\"Image gallery\",\"Camera\",\"Contact\",\"Audio\"]");

            return new NamedField(fieldName, FieldTypes.FILE_FIELD, fileUploadConfig, Map.of());
        }

        if (fieldType.equals(FieldTypes.LIST_FIELD)) {
            Map<String, Object> attributes = new HashMap<>();
            // Directly use a JSON string instead of converting a map
            attributes.put("field", "{\"type\":\"Field\",\"fieldType\":\"com.krista.fields.Text\",\"attributes\":{}}");
            return new NamedField(fieldName, FieldTypes.LIST_FIELD, attributes, Map.of());
        }

        if (fieldType.equals(FieldTypes.RICH_TEXT_FIELD)) {
            return createRichTextField(fieldName);
        }

        return new NamedField(fieldName, fieldType, Map.of(), Map.of());
    }
}
