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

package app.krista.extensions.essentials.documents.pdf.catalog.validators;

import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.FieldTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Validator for rich text content.
 * Validates that the provided rich text content is not empty and meets minimum requirements.
 */
public class RichTextValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(RichTextValidator.class);
    private static final int MIN_CONTENT_LENGTH = 5; // Minimum content length to be considered valid

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        return isValidRichText(resourceId);
    }

    private boolean isValidRichText(String content) {
        if (content == null || content.isBlank()) {
            logger.warn("Validation failed: Rich text content is null or blank");
            return false;
        }

        // Remove HTML tags to check actual content length
        String plainText = content.replaceAll("<[^>]*>", "").trim();
        
        if (plainText.length() < MIN_CONTENT_LENGTH) {
            logger.warn("Validation failed: Rich text content is too short ({})", plainText.length());
            return false;
        }
        
        return true;
    }

    @Override
    public String getFetchFieldName() {
        return "Rich Text Data";
    }

    @Override
    public String getFieldType() {
        return FieldTypes.RICH_TEXT_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please provide rich text content with at least " + MIN_CONTENT_LENGTH + " characters.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return "The rich text content is empty or too short. Please provide content with at least " + MIN_CONTENT_LENGTH + " characters.";
    }

    @Override
    public String getErrMessage(String resourceId) {
        return "Rich text content cannot be empty or blank. Please provide meaningful content with at least " + MIN_CONTENT_LENGTH + " characters.";
    }
}