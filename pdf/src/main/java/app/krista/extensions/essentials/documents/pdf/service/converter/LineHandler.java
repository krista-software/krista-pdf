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

/**
 * Strategy interface for handling different types of text lines during HTML conversion.
 * Implementations process specific line types (headings, lists, paragraphs).
 */
public interface LineHandler {

    /**
     * Checks if this handler can process the given line type.
     *
     * @param type the line type to check
     * @return true if this handler can process the type
     */
    boolean canHandle(LineType type);

    /**
     * Processes a line and appends HTML to the context.
     *
     * @param line the text line to process
     * @param context the HTML generation context
     */
    void handle(String line, HtmlGenerationContext context);
}
