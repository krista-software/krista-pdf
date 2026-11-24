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

import app.krista.extensions.essentials.documents.pdf.util.PdfSplitUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class responsible for splitting PDF documents.
 *
 * <p>This class follows the Single Responsibility Principle (SRP) by focusing
 * exclusively on PDF splitting operations. It was extracted from PDFManager
 * to reduce class size and improve maintainability.</p>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Split PDF at specific page number</li>
 *   <li>Automatic resource management with try-with-resources</li>
 *   <li>Early validation (fail fast)</li>
 *   <li>Proper error handling</li>
 * </ul>
 *
 * @author Krista Extensions Team
 * @since 2.0.12
 */
@Service
public class PdfSplitter {

    private static final Logger logger = LoggerFactory.getLogger(PdfSplitter.class);

    /**
     * Splits a PDF file at a specific page number into two files.
     *
     * <p>For example, a 10-page PDF with splitAtPage=6 will produce:</p>
     * <ul>
     *   <li>File 1: Pages 1-5</li>
     *   <li>File 2: Pages 6-10</li>
     * </ul>
     *
     * @param pdfFile     The PDF file to split
     * @param splitAtPage The page number where the split should occur (1-based index).
     *                    This page will be the first page of the second file.
     * @return A list containing exactly two PDF files (first part and second part)
     * @throws IOException              If an error occurs during PDF processing
     * @throws IllegalArgumentException If splitAtPage is invalid (less than 2 or greater than total pages)
     */
    public List<File> splitAtPageNumber(File pdfFile, int splitAtPage) throws IOException {
        logger.debug("Splitting PDF '{}' at page {}", pdfFile.getName(), splitAtPage);

        // Early validation - fail fast before loading PDF
        validateSplitPageMinimum(splitAtPage);

        // Load source once just to get the page count, then close it
        int numberOfPages;
        try (PDDocument document = PDDocument.load(pdfFile)) {
            numberOfPages = document.getNumberOfPages();
            logger.debug("PDF has {} pages", numberOfPages);
        }

        // Validate against actual page count
        validateSplitPageMaximum(splitAtPage, numberOfPages);

        // Generate file metadata
        String baseName = extractBaseName(pdfFile.getName());
        long timestamp = System.currentTimeMillis();

        // Split the document using reload-and-remove
        List<File> result = createSplitFiles(pdfFile, splitAtPage, numberOfPages, baseName, timestamp);

        logger.info("Successfully split PDF '{}' into {} files", pdfFile.getName(), result.size());
        return result;
    }

    /**
     * Validates that split page number meets minimum requirement.
     *
     * @param splitAtPage The page number to validate
     * @throws IllegalArgumentException If splitAtPage is less than 2
     */
    private void validateSplitPageMinimum(int splitAtPage) {
        if (splitAtPage < 2) {
            String errorMsg = String.format("Split page number must be at least 2. Provided: %d", splitAtPage);
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * Validates that split page number doesn't exceed total pages.
     *
     * @param splitAtPage The page number to validate
     * @param totalPages  The total number of pages in the document
     * @throws IllegalArgumentException If splitAtPage exceeds totalPages
     */
    private void validateSplitPageMaximum(int splitAtPage, int totalPages) {
        if (splitAtPage > totalPages) {
            String errorMsg = String.format(
                    "Split page number (%d) cannot be greater than total pages (%d)",
                    splitAtPage, totalPages
            );
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * Extracts the base name from a file name (removes extension).
     *
     * @param fileName The file name to process
     * @return The base name without extension
     */
    private String extractBaseName(String fileName) {
        return fileName.replaceFirst("[.][^.]+$", "");
    }

    /**
     * Creates the split PDF files using reload-and-remove strategy.
     *
     * @param sourceFile  The source PDF file on disk
     * @param splitAtPage The page number where the split occurs
     * @param totalPages  Total number of pages in the source document
     * @param baseName    Base name for output files
     * @param timestamp   Timestamp for unique file naming
     * @return List containing the two split PDF files
     * @throws IOException If an error occurs during file creation
     */
    private List<File> createSplitFiles(File sourceFile, int splitAtPage,
                                        int totalPages, String baseName, long timestamp) throws IOException {
        List<File> outputFiles = new ArrayList<>();

        // Create first part (pages 1 to splitAtPage-1)
        File firstPart = createPdfPart(sourceFile, 0, splitAtPage - 1,
                baseName, timestamp, 1, splitAtPage - 1);
        outputFiles.add(firstPart);
        logger.debug("Created first part: {}", firstPart.getName());

        // Create second part (pages splitAtPage to end)
        File secondPart = createPdfPart(sourceFile, splitAtPage - 1, totalPages,
                baseName, timestamp, splitAtPage, totalPages);
        outputFiles.add(secondPart);
        logger.debug("Created second part: {}", secondPart.getName());

        return outputFiles;
    }

    /**
     * Creates a single PDF part from a range of pages using reload-and-remove.
     *
     * @param sourceFile     The source PDF file on disk
     * @param startIndex     Zero-based start index (inclusive)
     * @param endIndex       Zero-based end index (exclusive)
     * @param baseName       Base name for the output file
     * @param timestamp      Timestamp for unique file naming
     * @param startPageLabel Human-readable start page number (1-based)
     * @param endPageLabel   Human-readable end page number (1-based)
     * @return The created PDF file
     * @throws IOException If an error occurs during file creation
     */
    private File createPdfPart(File sourceFile, int startIndex, int endIndex,
                               String baseName, long timestamp,
                               int startPageLabel, int endPageLabel) throws IOException {
        try (PDDocument partDocument = PdfSplitUtils.createDocumentWithPageRange(sourceFile, startIndex, endIndex)) {
            // Generate file name and save
            String fileName = String.format("%s_%d_pages_%d_to_%d.pdf",
                    baseName, timestamp, startPageLabel, endPageLabel);
            File outputFile = new File(fileName);
            partDocument.save(outputFile);

            logger.debug("Created PDF part: {} ({} pages)", fileName, endIndex - startIndex);

            return outputFile;
        }
    }
}

