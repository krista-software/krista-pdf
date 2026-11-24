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
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PDF2DOMConverter#convertPdfToHtml(File, boolean)}.
 *
 * <p>Branches exercised (identified by probing real sample PDFs under
 * {@code src/test/resources/pdf_samples} with ObjectExtractor +
 * SpreadsheetExtractionAlgorithm before writing these tests):</p>
 * <ul>
 *   <li>page loop: zero tables found at all (ADP sample)</li>
 *   <li>page loop: tables found but filtered out because they don't meet the
 *       rows&gt;=2 &amp;&amp; cols&gt;=2 threshold, both via a 1-col table (AllPay
 *       sample, maxCols=1) and a 1-row table (Paycom sample page 1, extracted into
 *       its own single-page fixture)</li>
 *   <li>page loop: a table that DOES meet the rows&gt;=2 &amp;&amp; cols&gt;=2
 *       threshold and is rendered as an HTML &lt;table&gt; (QBO sample)</li>
 *   <li>multi-page iteration of the page-processing for-loop (synthetic 3-page PDF)</li>
 *   <li>the outer try/catch -&gt; wrapped IOException branch on a corrupt/invalid PDF</li>
 * </ul>
 *
 * <p>NOT exercised (confirmed dead/unreachable, not just hard to drive):</p>
 * <ul>
 *   <li>the defensive {@code page != null} false branch - requires
 *       {@code technology.tabula.ObjectExtractor#extract(int)} to return null for a
 *       valid in-range page index, which only its internal implementation controls;
 *       {@code ObjectExtractor} is constructed internally (not injectable) so this
 *       cannot be driven without bytecode-level mocking of a concrete library class</li>
 *   <li>the {@code !table.getRows().isEmpty()} false outcome inside the same
 *       compound condition - only reachable when {@code rows.size() >= 2} already
 *       evaluated true, at which point {@code isEmpty()} is mathematically guaranteed
 *       false, making its own false outcome logically impossible to hit</li>
 * </ul>
 */
@DisplayName("PDF2DOMConverter Tests")
class PDF2DOMConverterTest {

    private PDF2DOMConverter converter;
    private final List<File> filesToCleanup = new ArrayList<>();

    @BeforeEach
    void setUp() {
        converter = new PDF2DOMConverter();
    }

    @AfterEach
    void tearDown() {
        for (File f : filesToCleanup) {
            if (f != null && f.exists()) {
                f.delete();
            }
        }
        filesToCleanup.clear();
    }

    @Test
    @DisplayName("Should convert a simple single-page text PDF with no tables")
    void testConvertPdfToHtml_SimpleSinglePage_NoTables() throws IOException {
        File pdf = createTextOnlyPdf(1);
        filesToCleanup.add(pdf);

        String html = converter.convertPdfToHtml(pdf, false);

        assertNotNull(html);
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("<body>"));
        assertTrue(html.contains("</html>"));
        assertFalse(html.contains("<table>"), "A plain text-only PDF should not produce any HTML tables");
    }

    @Test
    @DisplayName("Should convert a multi-page text PDF, iterating the page loop more than once")
    void testConvertPdfToHtml_MultiPage_NoTables() throws IOException {
        File pdf = createTextOnlyPdf(3);
        filesToCleanup.add(pdf);

        String html = converter.convertPdfToHtml(pdf, false);

        assertNotNull(html);
        assertTrue(html.contains("Page 1 of 3"));
        assertTrue(html.contains("Page 2 of 3"));
        assertTrue(html.contains("Page 3 of 3"));
    }

    @Test
    @DisplayName("Should derive the HTML title from the PDF file name")
    void testConvertPdfToHtml_TitleDerivedFromFileName() throws IOException {
        File pdf = createTextOnlyPdf(1);
        filesToCleanup.add(pdf);

        String html = converter.convertPdfToHtml(pdf, false);

        String expectedTitle = pdf.getName().replace(".pdf", "").replace(".PDF", "");
        assertTrue(html.contains("<title>" + expectedTitle + "</title>"));
        assertTrue(html.contains("Source document: " + pdf.getName()));
    }

    @Test
    @DisplayName("Should render a real table as HTML <table> when it meets the rows>=2 & cols>=2 threshold")
    void testConvertPdfToHtml_SampleWithValidTable_RendersHtmlTable() throws IOException {
        File pdf = sampleFile("QBO_R31368_Payroll Tax and wage summary.pdf");

        String html = converter.convertPdfToHtml(pdf, false);

        assertNotNull(html);
        assertTrue(html.contains("<table>"),
                "QBO sample has a confirmed 13x5 table that should be rendered as HTML");
        assertTrue(html.contains("</table>"));
    }

    @Test
    @DisplayName("Should filter out detected tables that don't meet the minimum rows/cols threshold")
    void testConvertPdfToHtml_SampleWithDegenerateTables_FiltersThemOut() throws IOException {
        File pdf = sampleFile("AllPay_R31328_Pre-Process Register.pdf");

        String html = converter.convertPdfToHtml(pdf, false);

        assertNotNull(html);
        // This sample's detected tables all have maxCols=1, so the validTables
        // filter (rows>=2 && cols>=2) rejects every one of them.
        assertFalse(html.contains("<table>"),
                "AllPay sample's detected tables are 1-column and should all be filtered out");
    }

    @Test
    @DisplayName("Should filter out a detected table that has fewer than 2 rows (rows>=2 false branch)")
    void testConvertPdfToHtml_SingleRowTable_FiltersItOut() throws IOException {
        // Page 1 of this sample was confirmed (by direct ObjectExtractor +
        // SpreadsheetExtractionAlgorithm probing) to yield several 1-row/1-col
        // "tables". Extracting just that one page into a small fixture lets us hit
        // the table.getRows().size() >= 2 false branch without converting the
        // full 197-page source document.
        File singlePage = extractFirstPageAsNewPdf(
                "src/test/resources/pdf_samples/Paycom_44271_Wage_And_Tax_Report.pdf");
        filesToCleanup.add(singlePage);

        String html = converter.convertPdfToHtml(singlePage, false);

        assertNotNull(html);
        assertFalse(html.contains("<table>"),
                "A 1-row detected table should be filtered out by the rows>=2 check");
    }

    @Test
    @DisplayName("Should handle a sample PDF where no tables are detected on any page")
    void testConvertPdfToHtml_SampleWithNoTablesDetected() throws IOException {
        File pdf = sampleFile("ADP_78105_4JN_20252_01412_Wage_and_Tax_Register.pdf");

        String html = converter.convertPdfToHtml(pdf, false);

        assertNotNull(html);
        assertFalse(html.contains("<table>"));
        assertTrue(html.length() > 0);
    }

    @Test
    @DisplayName("Should wrap any conversion failure (corrupt PDF) in an IOException")
    void testConvertPdfToHtml_CorruptPdf_ThrowsWrappedIOException() throws IOException {
        File corrupt = File.createTempFile("corrupt_", ".pdf");
        filesToCleanup.add(corrupt);
        try (FileWriter writer = new FileWriter(corrupt)) {
            writer.write("this is not a valid PDF file at all");
        }

        IOException ex = assertThrows(IOException.class, () -> converter.convertPdfToHtml(corrupt, false));
        assertTrue(ex.getMessage().contains("Failed to convert PDF"),
                "Exception message should come from the catch block's wrapping logic");
    }

    @Test
    @DisplayName("Should wrap a failure caused by a non-existent file in an IOException")
    void testConvertPdfToHtml_NonExistentFile_ThrowsWrappedIOException() {
        File missing = new File("/tmp/does-not-exist-pdf2dom-" + System.nanoTime() + ".pdf");

        IOException ex = assertThrows(IOException.class, () -> converter.convertPdfToHtml(missing, false));
        assertTrue(ex.getMessage().contains("Failed to convert PDF"));
    }

    // ==================== Helper Methods ====================

    private File sampleFile(String name) {
        File f = new File("src/test/resources/pdf_samples", name);
        assertTrue(f.exists(), "Expected sample fixture to exist: " + f.getAbsolutePath());
        return f;
    }

    /**
     * Extracts just the first page of the given source PDF into a new, small
     * single-page PDF file. Used to exercise page-specific table-detection
     * behavior from a large real-world fixture without paying the cost of
     * converting every page of the original document.
     */
    private File extractFirstPageAsNewPdf(String sourcePath) throws IOException {
        File output = File.createTempFile("pdf2dom_extracted_page_", ".pdf");
        try (PDDocument source = PDDocument.load(new File(sourcePath));
             PDDocument target = new PDDocument()) {
            // importPage() (rather than addPage()) deep-copies the page's resources
            // into the target document, since the two PDDocuments are independent
            // COS object graphs.
            target.importPage(source.getPage(0));
            target.save(output);
        }
        return output;
    }

    private File createTextOnlyPdf(int pageCount) throws IOException {
        File file = File.createTempFile("pdf2dom_text_test_", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(50, 700);
                    cs.showText("Page " + (i + 1) + " of " + pageCount);
                    cs.newLineAtOffset(0, -20);
                    cs.showText("This is plain paragraph text with no table structure.");
                    cs.endText();
                }
            }
            doc.save(file);
        }
        return file;
    }
}
