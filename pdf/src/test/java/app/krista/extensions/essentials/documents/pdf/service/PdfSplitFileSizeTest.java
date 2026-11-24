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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for PDF split file-size bloat.
 *
 * <p>Creates a multi-page PDF that shares a large image resource across all pages,
 * then splits it and verifies that no individual split file is larger than the
 * original and that the total size of all splits stays reasonable.</p>
 */
@DisplayName("PDF Split File Size Regression Tests")
class PdfSplitFileSizeTest {

    private final List<File> filesToCleanup = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (File f : filesToCleanup) {
            if (f != null && f.exists()) {
                f.delete();
            }
        }
    }

    @Test
    @DisplayName("splitPDF: no single-page split should exceed original file size")
    void splitPDF_singlePageShouldNotExceedOriginal() throws IOException {
        File testPdf = createPdfWithSharedImage(10);
        filesToCleanup.add(testPdf);
        long originalSize = testPdf.length();

        PDFManager pdfManager = new PDFManager(new PdfSplitter());
        List<File> splits = pdfManager.splitPDF(testPdf);
        filesToCleanup.addAll(splits);

        for (File split : splits) {
            assertTrue(split.length() <= originalSize,
                    String.format("Split file %s (%d bytes) should not exceed original (%d bytes)",
                            split.getName(), split.length(), originalSize));
        }
    }

    @Test
    @DisplayName("splitPDF: sum of all splits should be less than N x original size")
    void splitPDF_totalSizeShouldBeReasonable() throws IOException {
        // With importPage (old approach), every single-page split contains ALL shared resources
        // from the original → each split ≈ original size → total ≈ N × original.
        // With reload-and-remove (new approach), each page only carries resources it actually
        // uses, so the total should be well below N × original.
        int pageCount = 10;
        File testPdf = createPdfWithSharedImage(pageCount);
        filesToCleanup.add(testPdf);
        long originalSize = testPdf.length();

        PDFManager pdfManager = new PDFManager(new PdfSplitter());
        List<File> splits = pdfManager.splitPDF(testPdf);
        filesToCleanup.addAll(splits);

        long totalSplitSize = splits.stream().mapToLong(File::length).sum();
        long worstCaseWithOldApproach = (long) pageCount * originalSize;
        assertTrue(totalSplitSize < worstCaseWithOldApproach,
                String.format("Total split size (%d bytes) should be less than N×original (%d bytes). "
                                + "Old importPage approach would produce ~%d bytes.",
                        totalSplitSize, worstCaseWithOldApproach, worstCaseWithOldApproach));
    }

    @Test
    @DisplayName("splitPDFIntoParts: each half should be smaller than original")
    void splitPDFIntoParts_halvesShouldBeSmallerThanOriginal() throws IOException {
        File testPdf = createPdfWithSharedImage(10);
        filesToCleanup.add(testPdf);
        long originalSize = testPdf.length();

        PDFManager pdfManager = new PDFManager(new PdfSplitter());
        List<File> splits = pdfManager.splitPDFIntoParts(testPdf, 1);
        filesToCleanup.addAll(splits);

        assertEquals(2, splits.size(), "Should produce two halves");
        for (File split : splits) {
            assertTrue(split.length() <= originalSize,
                    String.format("Split half %s (%d bytes) should not exceed original (%d bytes)",
                            split.getName(), split.length(), originalSize));
        }
    }

    @Test
    @DisplayName("splitPDFAtPageNumber: each part should be smaller than original")
    void splitPDFAtPageNumber_partsShouldBeSmallerThanOriginal() throws IOException {
        File testPdf = createPdfWithSharedImage(10);
        filesToCleanup.add(testPdf);
        long originalSize = testPdf.length();

        PDFManager pdfManager = new PDFManager(new PdfSplitter());
        List<File> splits = pdfManager.splitPDFAtPageNumber(testPdf, 6);
        filesToCleanup.addAll(splits);

        assertEquals(2, splits.size());
        for (File split : splits) {
            assertTrue(split.length() <= originalSize,
                    String.format("Split part %s (%d bytes) should not exceed original (%d bytes)",
                            split.getName(), split.length(), originalSize));
        }
    }

    /**
     * Creates a multi-page PDF where every page draws the same large embedded image.
     * This simulates the real-world scenario where shared resources (fonts, images)
     * are referenced from the Pages tree and get duplicated by importPage.
     */
    private File createPdfWithSharedImage(int pageCount) throws IOException {
        File tempImage = File.createTempFile("test_image_", ".png");
        filesToCleanup.add(tempImage);

        // Create a 400x400 image (~several hundred KB as PNG)
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 400, 400);
        // Add some varied content so it doesn't compress to nearly nothing
        g.setColor(Color.RED);
        for (int i = 0; i < 400; i += 10) {
            g.drawLine(0, i, 400, 400 - i);
        }
        g.dispose();
        ImageIO.write(img, "png", tempImage);

        File pdfFile = File.createTempFile("shared_image_pdf_", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            PDImageXObject pdImage = PDImageXObject.createFromFile(tempImage.getAbsolutePath(), doc);
            for (int i = 0; i < pageCount; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(pdImage, 50, 400, 200, 200);
                }
            }
            doc.save(pdfFile);
        }

        return pdfFile;
    }
}
