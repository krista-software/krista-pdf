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

package app.krista.extensions.essentials.documents.pdf.service;

import app.krista.extensions.essentials.documents.pdf.service.converter.HtmlTextProcessor;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;

import java.util.List;

/**
 * Processor for handling PDF table extraction and conversion to HTML.
 *
 * <p>This class provides utilities for processing tables extracted from PDF documents
 * using the Tabula library. It handles two main operations:</p>
 * <ul>
 *   <li>Removing table text from raw PDF text to avoid duplication</li>
 *   <li>Converting extracted tables to properly formatted HTML table elements</li>
 * </ul>
 *
 * <p>The processor ensures that table data is rendered cleanly in HTML format
 * without duplicating content that appears in both the raw text extraction
 * and the structured table extraction.</p>
 */
public class PdfTableProcessor {

    /**
     * Removes table text from raw PDF text to avoid duplication in the final HTML output.
     *
     * <p>When extracting content from PDFs, both raw text extraction and table extraction
     * can capture the same content. This method identifies and removes lines from the raw
     * text that contain table cell content, ensuring tables are only rendered once as
     * proper HTML table elements.</p>
     *
     * <p><strong>Algorithm:</strong></p>
     * <ol>
     *   <li>Collects all cell text from all tables (including individual words from multi-word cells)</li>
     *   <li>Splits raw text into lines</li>
     *   <li>Removes any line that contains table cell text (case-insensitive matching)</li>
     *   <li>Preserves empty lines for document structure</li>
     * </ol>
     *
     * <p><strong>Note:</strong> This method uses aggressive matching - any line containing
     * table cell text (or even individual words from cells) will be removed. Words shorter
     * than 3 characters are excluded from matching to avoid over-aggressive removal.</p>
     *
     * @param rawText the raw text extracted from the PDF page, may contain table content
     * @param tables  the list of tables extracted from the same PDF page using Tabula
     * @return the raw text with all table-related lines removed, preserving empty lines
     * @throws NullPointerException if rawText is null
     * @see #tableToHtml(Table)
     * @see PDF2DOMConverter#convertPdfToHtml(java.io.File, boolean)
     */
    public String removeTableTextFromRawText(String rawText, List<Table> tables) {
        if (tables == null || tables.isEmpty()) {
            return rawText;
        }

        // Collect all cell text from all tables (including partial matches)
        java.util.Set<String> tableCellTexts = new java.util.HashSet<>();

        for (Table table : tables) {
            for (List<RectangularTextContainer> row : table.getRows()) {
                for (RectangularTextContainer cell : row) {
                    String cellText = cell.getText().trim();
                    if (!cellText.isEmpty()) {
                        tableCellTexts.add(cellText.toLowerCase());
                        // Also add individual words from multi-word cells
                        String[] cellWords = cellText.split("\\s+");
                        for (String word : cellWords) {
                            if (word.length() > 2) { // Only add words longer than 2 chars
                                tableCellTexts.add(word.toLowerCase().trim());
                            }
                        }
                    }
                }
            }
        }

        // Remove lines that contain ANY table cell text
        String[] lines = rawText.split("\\R");
        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();

            // Skip empty lines - keep them
            if (trimmedLine.isEmpty()) {
                result.append(line).append("\n");
                continue;
            }

            // Check if this line contains any table cell text
            String lowerLine = trimmedLine.toLowerCase();
            boolean isTableLine = false;

            // Check if any table cell text appears in this line
            for (String cellText : tableCellTexts) {
                if (lowerLine.contains(cellText)) {
                    isTableLine = true;
                    break;
                }
            }

            // Only keep lines that don't contain table text
            if (!isTableLine) {
                result.append(line).append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Converts a Tabula-extracted table to properly formatted HTML table markup.
     *
     * <p>This method generates semantic HTML table structure with proper thead/tbody
     * sections. The first row of the table is treated as the header row and rendered
     * using {@code <th>} elements within a {@code <thead>} section. All subsequent
     * rows are rendered as data rows using {@code <td>} elements within a {@code <tbody>}
     * section.</p>
     *
     * <p><strong>Note:</strong> All cell text is HTML-escaped using
     * {@link HtmlTextProcessor#escapeHtml(String)} to ensure safe rendering
     * and prevent injection attacks.</p>
     *
     * @param table the Tabula table object containing extracted table data from PDF
     * @return HTML string representation of the table with proper thead/tbody structure,
     * indented and ready for insertion into an HTML document
     * @throws NullPointerException if table is null
     */
    public String tableToHtml(Table table) {
        StringBuilder html = new StringBuilder();
        html.append("    <table>\n");
        List<List<RectangularTextContainer>> rows = table.getRows();
        boolean isFirstRow = true;
        boolean hasDataRows = false;
        for (List<RectangularTextContainer> row : rows) {
            if (isFirstRow) {
                html.append("      <thead>\n");
                html.append("        <tr>\n");
                for (RectangularTextContainer cell : row) {
                    html.append("          <th>").append(HtmlTextProcessor.escapeHtml(cell.getText()))
                            .append("</th>\n");
                }
                html.append("        </tr>\n");
                html.append("      </thead>\n");
                isFirstRow = false;
            } else {
                if (!hasDataRows) {
                    html.append("      <tbody>\n");
                    hasDataRows = true;
                }
                html.append("        <tr>\n");
                for (RectangularTextContainer cell : row) {
                    html.append("          <td>").append(HtmlTextProcessor.escapeHtml(cell.getText()))
                            .append("</td>\n");
                }
                html.append("        </tr>\n");
            }
        }
        if (hasDataRows) {
            html.append("      </tbody>\n");
        }
        html.append("    </table>\n\n");

        return html.toString();
    }
}
