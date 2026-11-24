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

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PDFManager split methods: splitPDF() and splitPDFIntoParts().
 * Covers the refactored reload-and-remove strategy using PdfSplitUtils.
 */
@DisplayName("PDFManager Split Methods Tests")
class PDFManagerSplitTest {

    private PDFManager pdfManager;
    private final List<File> filesToCleanup = new ArrayList<>();

    @BeforeEach
    void setUp() {
        pdfManager = new PDFManager(new PdfSplitter());
    }

    @AfterEach
    void tearDown() {
        for (File f : filesToCleanup) {
            if (f != null && f.exists()) {
                f.delete();
            }
        }
    }

    // ==================== splitPDF() Tests ====================

    @Test
    @DisplayName("splitPDF: should produce one file per page")
    void splitPDF_shouldProduceOneFilePerPage() throws IOException {
        int pageCount = 5;
        File testPdf = createTestPdf(pageCount);
        filesToCleanup.add(testPdf);

        List<File> splits = pdfManager.splitPDF(testPdf);
        filesToCleanup.addAll(splits);

        assertEquals(pageCount, splits.size(),
                "Should produce exactly one file per page");
    }

    @Test
    @DisplayName("splitPDF: each output file should contain exactly one page")
    void splitPDF_eachOutputShouldHaveOnePage() throws IOException {
        File testPdf = createTestPdf(4);
        filesToCleanup.add(testPdf);

        List<File> splits = pdfManager.splitPDF(testPdf);
        filesToCleanup.addAll(splits);

        for (int i = 0; i < splits.size(); i++) {
            try (PDDocument doc = PDDocument.load(splits.get(i))) {
                assertEquals(1, doc.getNumberOfPages(),
                        String.format("Split file %d should contain exactly 1 page", i + 1));
            }
        }
    }

    @Test
    @DisplayName("splitPDF: all output files should exist and be non-empty")
    void splitPDF_allFilesShouldExist() throws IOException {
        File testPdf = createTestPdf(3);
        filesToCleanup.add(testPdf);

        List<File> splits = pdfManager.splitPDF(testPdf);
        filesToCleanup.addAll(splits);

        for (File split : splits) {
            assertTrue(split.exists(), "Split file should exist: " + split.getName());
            assertTrue(split.length() > 0, "Split file should be non-empty: " + split.getName());
        }
    }

    @Test
    @DisplayName("splitPDF: output filenames should contain page numbers")
    void splitPDF_filenamesShouldContainPageNumbers() throws IOException {
        File testPdf = createTestPdf(3);
        filesToCleanup.add(testPdf);

        List<File> splits = pdfManager.splitPDF(testPdf);
        filesToCleanup.addAll(splits);

        for (int i = 0; i < splits.size(); i++) {
            assertTrue(splits.get(i).getName().contains("page_" + (i + 1)),
                    String.format("Filename should contain 'page_%d'", i + 1));
        }
    }

    @Test
    @DisplayName("splitPDF: single-page PDF should produce one file")
    void splitPDF_singlePagePdf() throws IOException {
        File testPdf = createTestPdf(1);
        filesToCleanup.add(testPdf);

        List<File> splits = pdfManager.splitPDF(testPdf);
        filesToCleanup.addAll(splits);

        assertEquals(1, splits.size(), "Single-page PDF should produce exactly 1 file");
        try (PDDocument doc = PDDocument.load(splits.get(0))) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("splitPDF: no single split should exceed original file size")
    void splitPDF_splitsShouldNotExceedOriginalSize() throws IOException {
        File testPdf = createTestPdf(5);
        filesToCleanup.add(testPdf);
        long originalSize = testPdf.length();

        List<File> splits = pdfManager.splitPDF(testPdf);
        filesToCleanup.addAll(splits);

        for (File split : splits) {
            assertTrue(split.length() <= originalSize,
                    String.format("Split %s (%d bytes) should not exceed original (%d bytes)",
                            split.getName(), split.length(), originalSize));
        }
    }

    @Test
    @DisplayName("splitPDF: output filenames should preserve base name")
    void splitPDF_shouldPreserveBaseName() throws IOException {
        File testPdf = createTestPdf(2);
        filesToCleanup.add(testPdf);

        List<File> splits = pdfManager.splitPDF(testPdf);
        filesToCleanup.addAll(splits);

        String baseName = testPdf.getName().replaceFirst("[.][^.]+$", "");
        for (File split : splits) {
            assertTrue(split.getName().startsWith(baseName),
                    "Output filename should start with original base name");
        }
    }

    @Test
    @DisplayName("splitPDF: PDF with 101 pages (> threshold) routes to splitWithImportPage strategy")
    void splitPDF_pdfAboveThreshold_usesImportPageStrategy() throws IOException {
        // SPLIT_SIZE_THRESHOLD = 100; 101 pages is the minimum to exercise splitWithImportPage.
        // Blank pages keep the file tiny so the test stays fast despite 101 iterations.
        int pageCount = 101;
        File testPdf = createTestPdf(pageCount);
        filesToCleanup.add(testPdf);

        List<File> splits = pdfManager.splitPDF(testPdf);
        filesToCleanup.addAll(splits);

        assertEquals(pageCount, splits.size(),
                "importPage strategy should produce one file per page");
        for (File f : splits) {
            try (PDDocument doc = PDDocument.load(f)) {
                assertEquals(1, doc.getNumberOfPages(),
                        "Each file from importPage strategy must contain exactly 1 page");
            }
        }
    }

    // ==================== splitPDFIntoParts() Tests ====================

    @Test
    @DisplayName("splitPDFIntoParts: should split into two halves when pages exceed threshold")
    void splitPDFIntoParts_shouldSplitAboveThreshold() throws IOException {
        File testPdf = createTestPdf(10);
        filesToCleanup.add(testPdf);

        List<File> splits = pdfManager.splitPDFIntoParts(testPdf, 5);
        filesToCleanup.addAll(splits);

        assertEquals(2, splits.size(), "Should produce exactly 2 halves");
    }

    @Test
    @DisplayName("splitPDFIntoParts: first half should have floor(n/2) pages")
    void splitPDFIntoParts_firstHalfPageCount() throws IOException {
        File testPdf = createTestPdf(10);
        filesToCleanup.add(testPdf);

        List<File> splits = pdfManager.splitPDFIntoParts(testPdf, 1);
        filesToCleanup.addAll(splits);

        try (PDDocument doc = PDDocument.load(splits.get(0))) {
            assertEquals(5, doc.getNumberOfPages(),
                    "First half of 10-page PDF should have 5 pages");
        }
    }

    @Test
    @DisplayName("splitPDFIntoParts: second half should have remaining pages")
    void splitPDFIntoParts_secondHalfPageCount() throws IOException {
        File testPdf = createTestPdf(10);
        filesToCleanup.add(testPdf);

        List<File> splits = pdfManager.splitPDFIntoParts(testPdf, 1);
        filesToCleanup.addAll(splits);

        try (PDDocument doc = PDDocument.load(splits.get(1))) {
            assertEquals(5, doc.getNumberOfPages(),
                    "Second half of 10-page PDF should have 5 pages");
        }
    }

    @Test
    @DisplayName("splitPDFIntoParts: odd page count should split correctly")
    void splitPDFIntoParts_oddPageCount() throws IOException {
        File testPdf = createTestPdf(7);
        filesToCleanup.add(testPdf);

        List<File> splits = pdfManager.splitPDFIntoParts(testPdf, 1);
        filesToCleanup.addAll(splits);

        assertEquals(2, splits.size());
        try (PDDocument first = PDDocument.load(splits.get(0));
             PDDocument second = PDDocument.load(splits.get(1))) {
            assertEquals(3, first.getNumberOfPages(), "First half should have floor(7/2)=3 pages");
            assertEquals(4, second.getNumberOfPages(), "Second half should have 4 pages");
        }
    }

    @Test
    @DisplayName("splitPDFIntoParts: total pages across halves should equal original")
    void splitPDFIntoParts_allPagesAccountedFor() throws IOException {
        File testPdf = createTestPdf(8);
        filesToCleanup.add(testPdf);

        List<File> splits = pdfManager.splitPDFIntoParts(testPdf, 1);
        filesToCleanup.addAll(splits);

        int totalPages = 0;
        for (File split : splits) {
            try (PDDocument doc = PDDocument.load(split)) {
                totalPages += doc.getNumberOfPages();
            }
        }
        assertEquals(8, totalPages, "Total pages across halves should equal original");
    }

    @Test
    @DisplayName("splitPDFIntoParts: should return original file when pages at threshold")
    void splitPDFIntoParts_atThreshold() throws IOException {
        File testPdf = createTestPdf(5);
        filesToCleanup.add(testPdf);

        List<File> result = pdfManager.splitPDFIntoParts(testPdf, 5);

        assertEquals(1, result.size(), "Should return original file when pages equal threshold");
        assertEquals(testPdf, result.get(0), "Should return the same file object");
    }

    @Test
    @DisplayName("splitPDFIntoParts: should return original file when pages below threshold")
    void splitPDFIntoParts_belowThreshold() throws IOException {
        File testPdf = createTestPdf(3);
        filesToCleanup.add(testPdf);

        List<File> result = pdfManager.splitPDFIntoParts(testPdf, 10);

        assertEquals(1, result.size(), "Should return original file when pages below threshold");
        assertEquals(testPdf, result.get(0), "Should return the same file object");
    }

    @Test
    @DisplayName("splitPDFIntoParts: halves should not exceed original file size")
    void splitPDFIntoParts_halvesShouldNotExceedOriginal() throws IOException {
        File testPdf = createTestPdf(10);
        filesToCleanup.add(testPdf);
        long originalSize = testPdf.length();

        List<File> splits = pdfManager.splitPDFIntoParts(testPdf, 1);
        filesToCleanup.addAll(splits);

        for (File split : splits) {
            assertTrue(split.length() <= originalSize,
                    String.format("Half %s (%d bytes) should not exceed original (%d bytes)",
                            split.getName(), split.length(), originalSize));
        }
    }

    @Test
    @DisplayName("splitPDFIntoParts: output files should be named first_half and second_half")
    void splitPDFIntoParts_outputFileNames() throws IOException {
        File testPdf = createTestPdf(6);
        filesToCleanup.add(testPdf);

        List<File> splits = pdfManager.splitPDFIntoParts(testPdf, 1);
        filesToCleanup.addAll(splits);

        assertEquals(PDFManager.FIRST_HALF, splits.get(0).getName(),
                "First file should be named 'first_half.pdf'");
        assertEquals(PDFManager.SECOND_HALF, splits.get(1).getName(),
                "Second file should be named 'second_half.pdf'");
    }

    @Test
    @DisplayName("splitPDFIntoParts: 2-page PDF above threshold=1 should split 1+1")
    void splitPDFIntoParts_twoPages() throws IOException {
        File testPdf = createTestPdf(2);
        filesToCleanup.add(testPdf);

        List<File> splits = pdfManager.splitPDFIntoParts(testPdf, 1);
        filesToCleanup.addAll(splits);

        assertEquals(2, splits.size());
        try (PDDocument first = PDDocument.load(splits.get(0));
             PDDocument second = PDDocument.load(splits.get(1))) {
            assertEquals(1, first.getNumberOfPages());
            assertEquals(1, second.getNumberOfPages());
        }
    }

    // ==================== Merge Optimization Tests ====================

    @Test
    @DisplayName("merge optimization: stripping unused resources should reduce merged file size")
    void mergeOptimization_shouldReduceMergedFileSize() throws IOException {
        // Create source PDFs with document-level overhead that accumulates during merge
        List<File> sourcePdfs = createSourcePdfsWithOverhead(5);
        filesToCleanup.addAll(sourcePdfs);

        // Merge without optimization
        File unoptimized = mergeFilesRaw(sourcePdfs);
        filesToCleanup.add(unoptimized);

        // Merge with optimization (same as production code)
        File optimized = mergeFilesOptimized(sourcePdfs);
        filesToCleanup.add(optimized);

        // Both should have all pages
        try (PDDocument unoptDoc = PDDocument.load(unoptimized);
             PDDocument optDoc = PDDocument.load(optimized)) {
            assertEquals(5, unoptDoc.getNumberOfPages());
            assertEquals(5, optDoc.getNumberOfPages(),
                    "Optimized file should contain all original pages");
        }

        // Optimized file should be smaller than unoptimized
        assertTrue(optimized.length() < unoptimized.length(),
                String.format("Optimized merge (%d bytes) should be smaller than unoptimized (%d bytes)",
                        optimized.length(), unoptimized.length()));
    }

    @Test
    @DisplayName("merge optimization: should preserve bookmarks and metadata from source files")
    void mergeOptimization_shouldPreserveDocumentLevelData() throws IOException {
        // Create source PDFs with bookmarks and metadata
        List<File> sourcePdfs = createSourcePdfsWithOverhead(3);
        filesToCleanup.addAll(sourcePdfs);

        // Merge with optimization
        File optimized = mergeFilesOptimized(sourcePdfs);
        filesToCleanup.add(optimized);

        try (PDDocument doc = PDDocument.load(optimized)) {
            assertEquals(3, doc.getNumberOfPages());

            // Bookmarks must survive the merge optimization
            assertNotNull(doc.getDocumentCatalog().getDocumentOutline(),
                    "Merge optimization must not strip bookmarks");

            // Metadata must survive the merge optimization
            assertNotNull(doc.getDocumentCatalog().getMetadata(),
                    "Merge optimization must not strip metadata");
        }
    }

    @Test
    @DisplayName("splitPDF then merge: merged file should preserve all pages and not bloat")
    void splitThenMerge_shouldPreserveAllPagesAndNotBloat() throws IOException {
        File testPdf = createTestPdfWithContent(5);
        filesToCleanup.add(testPdf);
        long originalSize = testPdf.length();

        List<File> splits = pdfManager.splitPDF(testPdf);
        filesToCleanup.addAll(splits);
        assertEquals(5, splits.size());

        // Merge with optimization + deduplication
        File mergedFile = mergeFilesOptimized(splits);
        filesToCleanup.add(mergedFile);

        try (PDDocument mergedDoc = PDDocument.load(mergedFile)) {
            assertEquals(5, mergedDoc.getNumberOfPages(),
                    "Merged file should contain all original pages");
        }

        // With deduplication, merged file should not be significantly larger
        // than the original (allow 10% tolerance for structural overhead)
        long maxAllowed = (long) (originalSize * 1.10);
        assertTrue(mergedFile.length() <= maxAllowed,
                String.format("Merged file (%d bytes) should not exceed 110%% of original (%d bytes, limit %d bytes)",
                        mergedFile.length(), originalSize, maxAllowed));
    }

    @Test
    @DisplayName("deduplication: should significantly reduce size vs raw merge of split files")
    void deduplication_shouldReduceSizeVsRawMerge() throws IOException {
        File testPdf = createTestPdfWithContent(10);
        filesToCleanup.add(testPdf);

        List<File> splits = pdfManager.splitPDF(testPdf);
        filesToCleanup.addAll(splits);

        File rawMerged = mergeFilesRaw(splits);
        filesToCleanup.add(rawMerged);

        File optimizedMerged = mergeFilesOptimized(splits);
        filesToCleanup.add(optimizedMerged);

        // Deduplication should produce a substantially smaller file
        assertTrue(optimizedMerged.length() < rawMerged.length(),
                String.format("Optimized merge (%d bytes) should be smaller than raw merge (%d bytes)",
                        optimizedMerged.length(), rawMerged.length()));
    }

    /**
     * Creates source PDFs that each have document-level overhead (bookmarks,
     * XMP metadata) which accumulates during merge and is stripped by
     * stripDocumentLevelOverhead().
     */
    private List<File> createSourcePdfsWithOverhead(int count) throws IOException {
        // Build a non-trivial XMP metadata block (~2KB per file)
        String xmp = "<?xpacket begin='\uFEFF' id='W5M0MpCehiHzreSzNTczkc9d'?>"
                + "<x:xmpmeta xmlns:x='adobe:ns:meta/'>"
                + "<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>"
                + "<rdf:Description rdf:about='' xmlns:dc='http://purl.org/dc/elements/1.1/'>"
                + "<dc:title><rdf:Alt><rdf:li xml:lang='x-default'>"
                + "A".repeat(500)  // pad to make metadata substantial
                + "</rdf:li></rdf:Alt></dc:title>"
                + "</rdf:Description>"
                + "</rdf:RDF></x:xmpmeta>"
                + "<?xpacket end='w'?>";
        byte[] xmpBytes = xmp.getBytes(StandardCharsets.UTF_8);

        List<File> files = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            File file = File.createTempFile("merge_source_", ".pdf");
            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(50, 700);
                    cs.showText("Source document " + (i + 1));
                    cs.endText();
                }

                // Add bookmarks (removed by stripDocumentLevelOverhead via OUTLINES)
                PDDocumentOutline outline = new PDDocumentOutline();
                for (int b = 0; b < 10; b++) {
                    PDOutlineItem item = new PDOutlineItem();
                    item.setTitle("Bookmark " + b + " in source " + i);
                    outline.addLast(item);
                }
                doc.getDocumentCatalog().setDocumentOutline(outline);

                // Add XMP metadata (removed by stripDocumentLevelOverhead via METADATA)
                PDMetadata metadata = new PDMetadata(doc);
                metadata.importXMPMetadata(xmpBytes);
                doc.getDocumentCatalog().setMetadata(metadata);

                doc.save(file);
            }
            files.add(file);
        }
        return files;
    }

    /** Merges files without any post-merge optimization. */
    private File mergeFilesRaw(List<File> files) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        File merged = File.createTempFile("merged_raw_", ".pdf");
        merger.setDestinationFileName(merged.getAbsolutePath());
        for (File f : files) {
            merger.addSource(f);
        }
        merger.mergeDocuments();
        return merged;
    }

    /** Merges files with the same post-merge optimization as production code. */
    private File mergeFilesOptimized(List<File> files) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        File merged = File.createTempFile("merged_optimized_", ".pdf");
        merger.setDestinationFileName(merged.getAbsolutePath());
        for (File f : files) {
            merger.addSource(f);
        }
        merger.mergeDocuments();

        try (PDDocument doc = PDDocument.load(merged)) {
            app.krista.extensions.essentials.documents.pdf.util.PdfSplitUtils.filterUnusedPageResources(doc);
            app.krista.extensions.essentials.documents.pdf.util.PdfSplitUtils.deduplicateResources(doc);
            doc.save(merged);
        }
        return merged;
    }

    // ==================== Helper Methods ====================

    private File createTestPdf(int pageCount) throws IOException {
        File file = File.createTempFile("pdf_manager_split_test_", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                doc.addPage(new PDPage());
            }
            doc.save(file);
        }
        return file;
    }

    /**
     * Creates a test PDF with an embedded image on every page, so that
     * split-then-merge produces measurable resource duplication. The same
     * image bytes appear once in the original but are copied into each
     * split file, then duplicated N times in the naive merge.
     */
    private File createTestPdfWithContent(int pageCount) throws IOException {
        // Create a non-trivial image (200x200 gradient) to embed on each page
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 200; y++) {
            for (int x = 0; x < 200; x++) {
                img.setRGB(x, y, (x << 16) | (y << 8) | 128);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        File file = File.createTempFile("pdf_manager_content_test_", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, imageBytes, "test_image");
            for (int i = 0; i < pageCount; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(pdImage, 50, 500, 200, 200);
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(50, 480);
                    cs.showText("Page " + (i + 1) + " of " + pageCount);
                    cs.endText();
                }
            }
            doc.save(file);
        }
        return file;
    }
}
