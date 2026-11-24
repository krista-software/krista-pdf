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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.Table;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * PDF to semantic HTML converter.
 * Extracts text from PDF and converts to clean, readable HTML with proper
 * structure.
 */
public class PDF2DOMConverter {

    private static final Logger logger = LoggerFactory.getLogger(PDF2DOMConverter.class);
    private final HtmlTextProcessor htmlProcessor = new HtmlTextProcessor();
    private final PdfTableProcessor tableProcessor = new PdfTableProcessor();

    /**
     * Constructs a new PDF2DOMConverter.
     */
    public PDF2DOMConverter() {
        // Default constructor
    }

    /**
     * Converts a PDF file to semantic HTML with table extraction.
     * Processes each page, extracts tables separately, and converts text to structured HTML.
     *
     * @param pdfFile the PDF file to convert
     * @param includeFormatting whether to include formatting (currently unused)
     * @return complete HTML document as a string
     * @throws IOException if PDF reading or conversion fails
     */
    public String convertPdfToHtml(File pdfFile, boolean includeFormatting) throws IOException {
        logger.info("Converting PDF to HTML with table extraction: {}", pdfFile.getName());

        try (PDDocument document = PDDocument.load(pdfFile)) {
            StringBuilder html = new StringBuilder();

            // Get title from filename
            String title = pdfFile.getName().replace(".pdf", "").replace(".PDF", "");

            // Build HTML header
            html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
                    .append("  <meta charset=\"UTF-8\"/>\n")
                    .append("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>\n")
                    .append("  <title>").append(HtmlTextProcessor.escapeHtml(title)).append("</title>\n")
                    .append("  <style>\n")
                    .append(getCSS())
                    .append("  </style>\n")
                    .append("</head>\n<body>\n\n")
                    .append("  <header>\n")
                    .append("    <h1>").append(HtmlTextProcessor.escapeHtml(title)).append("</h1>\n")
                    .append("    <p class=\"small\">Converted from PDF. Source document: ")
                    .append(HtmlTextProcessor.escapeHtml(pdfFile.getName())).append("</p>\n")
                    .append("  </header>\n\n")
                    .append("  <article>\n\n");

            // Process page by page
            int pageCount = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            ObjectExtractor extractor = new ObjectExtractor(document);
            SpreadsheetExtractionAlgorithm spreadsheetAlgo = new SpreadsheetExtractionAlgorithm();

            for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
                // Extract tables FIRST for this page using ONLY SpreadsheetExtractionAlgorithm
                // This is more conservative and only detects actual tables with clear cell
                // boundaries
                Page page = extractor.extract(pageIndex);
                List<Table> validTables = new java.util.ArrayList<>();

                if (page != null) {
                    List<Table> tables = spreadsheetAlgo.extract(page);

                    // Only keep tables with at least 2 rows and 2 columns
                    for (Table table : tables) {
                        if (table.getRows().size() >= 2 &&
                                !table.getRows().isEmpty() &&
                                table.getRows().get(0).size() >= 2) {
                            validTables.add(table);
                        }
                    }
                }

                // Extract text for this page
                stripper.setStartPage(pageIndex);
                stripper.setEndPage(pageIndex);
                String rawText = stripper.getText(document);

                // Remove table content from raw text to avoid duplication
                String textWithoutTables = tableProcessor.removeTableTextFromRawText(rawText, validTables);

                // Add text content (without table text)
                html.append(htmlProcessor.buildHtmlFromText(textWithoutTables));

                // Add tables as proper HTML tables
                for (Table table : validTables) {
                    html.append(tableProcessor.tableToHtml(table));
                }
            }
            html.append("\n  </article>\n\n")
                    .append("</body>\n</html>\n");

            String result = html.toString();
            logger.info("Converted PDF to HTML. Length: {} characters", result.length());
            return result;
        } catch (Exception e) {
            logger.error("Error converting PDF: {}", e.getMessage(), e);
            throw new IOException("Failed to convert PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the CSS stylesheet for the HTML output.
     * Provides professional styling with semantic colors and spacing.
     *
     * @return CSS string for embedding in HTML
     */
    private String getCSS() {
        return "    body { font-family: Arial, Helvetica, sans-serif; line-height: 1.45; color: #222; padding: 24px; max-width: 1000px; margin: auto; }\n"
                +
                "    h1,h2,h3 { color: #1b3c74; margin-top: 1.2em; margin-bottom: 0.5em; }\n" +
                "    h1 { font-size: 1.8em; }\n" +
                "    h2 { font-size: 1.3em; }\n" +
                "    p { margin: 0.6em 0; }\n" +
                "    ul, ol { margin: 0.6em 0 0.6em 1.6em; }\n" +
                "    table { width: 100%; border-collapse: collapse; margin: 1em 0; border: 1px solid #ddd; }\n" +
                "    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; vertical-align: top; }\n" +
                "    th { background: #f4f6fb; font-weight: bold; }\n" +
                "    .small { font-size: 0.9em; color: #666; }\n";
    }
}