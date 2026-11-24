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

package app.krista.extensions.essentials.documents.pdf.helper.strategy;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.model.base.File;

/**
 * Strategy interface for different validation approaches.
 *
 * <p>This interface follows the Strategy Pattern to allow different validation
 * strategies (with retry vs without retry) to be used interchangeably.</p>
 *
 * <p><b>SOLID Principles Applied:</b></p>
 * <ul>
 *   <li><b>Open/Closed Principle:</b> Open for extension (new strategies), closed for modification</li>
 *   <li><b>Dependency Inversion Principle:</b> Depend on abstraction, not concrete implementations</li>
 * </ul>
 */
public interface ValidationStrategy {

    /**
     * Validates the input parameters before processing.
     *
     * @param inputFile       The PDF file to validate
     * @param splitPageNumber The page number to validate
     * @return ValidationResult containing validation status and any errors
     */
    ValidationResult validate(File inputFile, Double splitPageNumber);

    /**
     * Handles validation failure according to the strategy.
     *
     * @param result          The validation result containing errors
     * @param inputFile       The input file
     * @param splitPageNumber The split page number
     * @return ExtensionResponse with appropriate error handling
     */
    ExtensionResponse handleValidationFailure(ValidationResult result, File inputFile, Double splitPageNumber);

    /**
     * Result of validation containing status and error details.
     */
    class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final ExtensionResponse.Error.ExceptionType exceptionType;
        private final Object additionalData;

        private ValidationResult(boolean valid, String errorMessage,
                                 ExtensionResponse.Error.ExceptionType exceptionType,
                                 Object additionalData) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.exceptionType = exceptionType;
            this.additionalData = additionalData;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null, null, null);
        }

        public static ValidationResult failure(String errorMessage,
                                               ExtensionResponse.Error.ExceptionType exceptionType) {
            return new ValidationResult(false, errorMessage, exceptionType, null);
        }

        public static ValidationResult failure(String errorMessage,
                                               ExtensionResponse.Error.ExceptionType exceptionType,
                                               Object additionalData) {
            return new ValidationResult(false, errorMessage, exceptionType, additionalData);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public ExtensionResponse.Error.ExceptionType getExceptionType() {
            return exceptionType;
        }

        public Object getAdditionalData() {
            return additionalData;
        }
    }
}

