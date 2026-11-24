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

import app.krista.extensions.essentials.documents.pdf.util.PdfSplitUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfSplitter {

    // Method to split the PDF based on your criteria
    public static List<File> splitPdfByCriteria(File inputFile) throws IOException {
        // Phase 1: determine section boundaries by analysing text
        List<int[]> sections = determineSections(inputFile);

        // Phase 2: create each section using reload-and-remove (no shared-resource bloat)
        List<File> generatedPdfFiles = new ArrayList<>();
        int partNumber = 1;
        for (int[] section : sections) {
            File newPdfFile = saveSplitDocument(inputFile, section[0], section[1], partNumber++);
            generatedPdfFiles.add(newPdfFile);
        }

        return generatedPdfFiles;
    }

    /**
     * Analyses the PDF text to determine section boundaries.
     * A section ends when the last line of a page matches "Total units = &lt;integer&gt;"
     * or contains "End of report".
     */
    private static List<int[]> determineSections(File inputFile) throws IOException {
        List<int[]> sections = new ArrayList<>();

        try (PDDocument document = PDDocument.load(inputFile)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            int startPage = 0;
            boolean inSection = false;
            String currentPageText;

            // Define the regex pattern to match "Total units = <integer>"
            Pattern pattern = Pattern.compile("Total units = \\d+");

            // Iterate through each page and determine sections based on criteria
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                pdfStripper.setStartPage(i + 1);
                pdfStripper.setEndPage(i + 1);
                currentPageText = pdfStripper.getText(document);

                // Split current page text into lines and check only the last line
                String[] lines = currentPageText.trim().split("\\r?\\n");
                String lastLine = lines[lines.length - 1];

                // Check if the last line matches the pattern or contains "End of report"
                Matcher matcher = pattern.matcher(lastLine);
                if (matcher.find() || lastLine.contains("End of report")) {
                    if (inSection) {
                        sections.add(new int[]{startPage, i});
                        inSection = false;
                    } else {
                        sections.add(new int[]{i, i}); // standalone section
                    }
                } else {
                    if (!inSection) {
                        startPage = i;
                        inSection = true;
                    }
                }
            }

            // Handle the last section if it is still open
            if (inSection) {
                sections.add(new int[]{startPage, document.getNumberOfPages() - 1});
            }
        }

        return sections;
    }

    // Helper method to save a specific range of pages as a separate PDF
    private static File saveSplitDocument(File sourceFile, int startPage, int endPage, int partNumber) throws IOException {
        // endPage is inclusive, createDocumentWithPageRange expects exclusive end
        try (PDDocument newDocument = PdfSplitUtils.createDocumentWithPageRange(sourceFile, startPage, endPage + 1)) {
            File outputFile = new File("pdf_part_" + partNumber + ".pdf");
            newDocument.save(outputFile);
            return outputFile;
        }
    }
}
