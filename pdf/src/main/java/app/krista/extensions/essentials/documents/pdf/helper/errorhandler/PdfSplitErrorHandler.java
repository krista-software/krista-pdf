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

import app.krista.extension.executor.ErrorBuilder;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.model.base.File;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Error handler for PDF split operations.
 *
 * <p>This class follows the Single Responsibility Principle by focusing solely
 * on error handling and response generation for different error scenarios.</p>
 *
 * <p><b>Design Patterns Applied:</b></p>
 * <ul>
 *   <li><b>Factory Pattern:</b> Creates appropriate error responses based on exception type</li>
 *   <li><b>Chain of Responsibility:</b> Handles different error types in sequence</li>
 * </ul>
 *
 * <p><b>SOLID Principles Applied:</b></p>
 * <ul>
 *   <li><b>Single Responsibility:</b> Only handles error response creation</li>
 *   <li><b>Open/Closed:</b> Open for extension (new error types), closed for modification</li>
 * </ul>
 */
@Service
public class PdfSplitErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(PdfSplitErrorHandler.class);

    // Error message constants
    private static final String CORRUPTED_PDF_MESSAGE =
            "The uploaded PDF file appears to be corrupted or invalid. " +
                    "Please ensure the file is a valid PDF document and try again.";

    private static final String PROCESSING_ERROR_MESSAGE =
            "Failed to process the PDF file: %s";

    private static final String UNEXPECTED_ERROR_MESSAGE =
            "An unexpected error occurred while processing the PDF: %s";

    private static final String GENERIC_SPLIT_ERROR_MESSAGE =
            "Failed to split PDF at page number: %s";

    /**
     * Handles exceptions and creates appropriate error responses.
     *
     * @param exception       The exception that occurred
     * @param inputFile       The input file being processed
     * @param splitPageNumber The split page number
     * @param useBuilder      Whether to use ExtensionResponseBuilder (true) or direct construction (false)
     * @return ExtensionResponse with error details
     */
    public ExtensionResponse handleException(Exception exception,
                                             File inputFile,
                                             Double splitPageNumber,
                                             boolean useBuilder) {
        if (exception instanceof IOException) {
            return handleIOException((IOException) exception, inputFile, splitPageNumber, useBuilder);
        } else if (exception instanceof IllegalArgumentException) {
            return handleIllegalArgumentException((IllegalArgumentException) exception, useBuilder);
        } else {
            return handleUnexpectedException(exception, useBuilder);
        }
    }

    /**
     * Handles IOException (file I/O errors, PDF corruption, etc.).
     */
    private ExtensionResponse handleIOException(IOException exception,
                                                File inputFile,
                                                Double splitPageNumber,
                                                boolean useBuilder) {
        logger.error("Failed to split PDF file '{}': {}",
                inputFile != null ? inputFile.getFileName() : "null",
                exception.getMessage(), exception);

        // Check for specific PDF corruption errors
        if (isPdfCorrupted(exception)) {
            return createErrorResponse(
                    CORRUPTED_PDF_MESSAGE,
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    useBuilder
            );
        } else {
            return createErrorResponse(
                    String.format(PROCESSING_ERROR_MESSAGE, exception.getMessage()),
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                    useBuilder
            );
        }
    }

    /**
     * Handles IllegalArgumentException (validation errors).
     */
    private ExtensionResponse handleIllegalArgumentException(IllegalArgumentException exception,
                                                             boolean useBuilder) {
        logger.error("Invalid input for split PDF: {}", exception.getMessage(), exception);

        return createErrorResponse(
                exception.getMessage(),
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                useBuilder
        );
    }

    /**
     * Handles unexpected exceptions.
     */
    private ExtensionResponse handleUnexpectedException(Exception exception, boolean useBuilder) {
        logger.error("Unexpected error splitting PDF: {}", exception.getMessage(), exception);

        return createErrorResponse(
                String.format(UNEXPECTED_ERROR_MESSAGE, exception.getMessage()),
                ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                useBuilder
        );
    }

    /**
     * Creates error response using appropriate method based on useBuilder flag.
     */
    private ExtensionResponse createErrorResponse(String message,
                                                  ExtensionResponse.Error.ExceptionType exceptionType,
                                                  boolean useBuilder) {
        if (useBuilder) {
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(exceptionType)
                    .errorMessage(message)
                    .build();

            return new ExtensionResponseBuilder().failure(error).build();
        } else {
            ExtensionResponse.Error error = new ExtensionResponse.Error(
                    message,
                    System.currentTimeMillis(),
                    exceptionType,
                    null
            );

            return new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error, null, null);
        }
    }

    /**
     * Checks if the IOException indicates a corrupted PDF.
     */
    private boolean isPdfCorrupted(IOException exception) {
        String message = exception.getMessage();
        return message != null && (
                message.contains("Missing root object specification") ||
                        message.contains("Header doesn't contain versioninfo") ||
                        message.contains("Error: Header doesn't contain versioninfo")
        );
    }
}

