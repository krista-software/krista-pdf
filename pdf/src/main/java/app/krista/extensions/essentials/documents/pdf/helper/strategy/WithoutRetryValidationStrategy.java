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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validation strategy without retry support.
 *
 * <p>This strategy performs minimal validation and returns direct error responses
 * without retry mechanism.</p>
 *
 * <p><b>SOLID Principles Applied:</b></p>
 * <ul>
 *   <li><b>Single Responsibility:</b> Only handles validation without retry logic</li>
 *   <li><b>Liskov Substitution:</b> Can be substituted for ValidationStrategy interface</li>
 * </ul>
 */
public class WithoutRetryValidationStrategy implements ValidationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(WithoutRetryValidationStrategy.class);

    @Override
    public ValidationResult validate(File inputFile, Double splitPageNumber) {
        if (splitPageNumber == null) {
            logger.error("Split page number is required");
            return ValidationResult.failure("Split page number is required", ExtensionResponse.Error.ExceptionType.INPUT_ERROR);
        }
        return ValidationResult.success();
    }

    @Override
    public ExtensionResponse handleValidationFailure(ValidationResult result, File inputFile, Double splitPageNumber) {
        logger.debug("Allow Retry is disabled, returning direct validation error");
        ExtensionResponse.Error error = new ExtensionResponse.Error(result.getErrorMessage(), System.currentTimeMillis(), result.getExceptionType(), null);
        return new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error, null, null);
    }
}