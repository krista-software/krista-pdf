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

import app.krista.model.base.File;

import java.io.IOException;

import static app.krista.extensions.essentials.documents.pdf.service.converter.HtmlOutputConstants.HTML_SUFFIX;

/**
 * Strategy interface for packaging HTML output in different formats.
 * 
 * <p>This interface defines the contract for different HTML output packaging strategies,
 * such as ZIP files or plain text files. Implementations handle the specific packaging
 * logic and file upload to Krista Media.</p>
 * 
 * <p><b>Design Pattern:</b> Strategy Pattern</p>
 * <ul>
 *   <li>Allows runtime selection of packaging format (ZIP vs TXT)</li>
 *   <li>Encapsulates packaging logic in separate strategy classes</li>
 *   <li>Makes it easy to add new output formats without modifying existing code</li>
 * </ul>
 * 
 * @see ZipHtmlOutputStrategy
 * @see TextHtmlOutputStrategy
 */
public interface HtmlOutputStrategy {
    
    /**
     * Packages the HTML file and uploads it to Krista Media.
     * 
     * @param htmlFile The HTML file to package
     * @param originalFileName The original PDF filename (used for naming the output)
     * @return File object representing the uploaded packaged file
     * @throws IOException If file I/O or upload fails
     */
    File packageAndUpload(java.io.File htmlFile, String originalFileName) throws IOException;
    
    /**
     * Gets the file extension for this output format.
     * 
     * @return File extension (e.g., "zip", "txt")
     */
    String getFileExtension();
    
    /**
     * Gets a human-readable description of this output format.
     *
     * @return Description of the output format
     */
    String getDescription();

    /**
     * Extracts the base filename without extension.
     *
     * @param originalFileName The original filename with extension
     * @return The filename without extension
     */
    default String extractBaseFileName(String originalFileName) {
        return originalFileName.replaceFirst("\\.[^.]+$", "");
    }

    /**
     * Generates the output filename with timestamp and extension.
     *
     * @param baseFileName The base filename without extension
     * @param timestamp The timestamp string
     * @param extension The file extension
     * @return The complete output filename
     */
    default String generateOutputFileName(String baseFileName, String timestamp, String extension) {
        return baseFileName + HTML_SUFFIX + timestamp + "." + extension;
    }
}

