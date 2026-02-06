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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

public class ExtensionResponseWrapper {
    private static final Logger log = LoggerFactory.getLogger(ExtensionResponseWrapper.class);

    /**
     * Converts a stack trace to a readable string format.
     *
     * @param throwable the throwable to extract stack trace from
     * @return formatted stack trace string
     */
    private static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    public static ExtensionResponse wrapResponse(Supplier<Map<String, Object>> supplier) {
        try {
            final var responseValue = supplier.get();
            return new ExtensionResponse(ExtensionResponse.Result.SUCCESS, responseValue, null, null, null);
        } catch (KristaException e) {
            log.error("Krista exception occurred while processing the request", e);
            final var error = new ExtensionResponse.Error(
                    e.getMessage(), Instant.now().toEpochMilli(),
                    e.getExceptionType(), getStackTraceAsString(e));

            return new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error, null, null);
        } catch (Exception e) {
            log.error("Error occurred while processing the request", e);
            final var error = new ExtensionResponse.Error(
                    e.getMessage(), Instant.now().toEpochMilli(),
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, getStackTraceAsString(e));

            return new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error, null, null);
        }
    }
}
