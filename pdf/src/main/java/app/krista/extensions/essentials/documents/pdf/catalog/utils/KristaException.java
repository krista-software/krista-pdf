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

import app.krista.extension.executor.ExtensionResponse.Error.ExceptionType;

public class KristaException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "An error occurred while processing the request";
    private static final ExceptionType DEFAULT_EXCEPTION_TYPE = ExceptionType.SYSTEM_ERROR;

    private final ExceptionType exceptionType;

    public KristaException() {
        super(DEFAULT_MESSAGE);
        this.exceptionType = DEFAULT_EXCEPTION_TYPE;
    }

    public KristaException(String message) {
        super(message);
        this.exceptionType = DEFAULT_EXCEPTION_TYPE;
    }

    public KristaException(String message, ExceptionType exceptionType) {
        super(message);
        this.exceptionType = exceptionType;
    }

    public KristaException(String message, Throwable cause, ExceptionType exceptionType) {
        super(message, cause);
        this.exceptionType = exceptionType;
    }

    public ExceptionType getExceptionType() {
        return exceptionType;
    }
}
