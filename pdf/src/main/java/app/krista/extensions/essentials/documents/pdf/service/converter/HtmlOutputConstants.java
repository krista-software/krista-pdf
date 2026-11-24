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

package app.krista.extensions.essentials.documents.pdf.service.converter;

import java.time.format.DateTimeFormatter;

/**
 * Constants shared across HTML output strategy implementations.
 * 
 * <p>This class provides common constants used by all {@link HtmlOutputStrategy}
 * implementations to ensure consistency in file naming, formatting, and behavior.</p>
 * 
 * @see HtmlOutputStrategy
 * @see ZipHtmlOutputStrategy
 * @see TextHtmlOutputStrategy
 */
public final class HtmlOutputConstants {
    
    /**
     * Timestamp formatter for output file names.
     * Format: yyyyMMdd_HHmmss (e.g., "20231215_143022")
     */
    public static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * Suffix added to output file names to indicate HTML content.
     */
    public static final String HTML_SUFFIX = "_html_";
    
    /**
     * Private constructor to prevent instantiation.
     */
    private HtmlOutputConstants() {
        throw new AssertionError("HtmlOutputConstants is a utility class and should not be instantiated");
    }
}

