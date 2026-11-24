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

package app.krista.extensions.essentials.documents.pdf.util;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PdfSplitUtils Unit Tests")
class PdfSplitUtilsTest {

    private File testPdf;
    private final List<File> filesToCleanup = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        testPdf = createBlankPdf(5);
        filesToCleanup.add(testPdf);
    }

    @AfterEach
    void tearDown() {
        for (File f : filesToCleanup) {
            if (f != null && f.exists()) {
                f.delete();
            }
        }
    }

    // =====================================================================
    //  createDocumentWithPages
    // =====================================================================
    @Nested
    @DisplayName("createDocumentWithPages")
    class CreateDocumentWithPages {

        @Test
        @DisplayName("Extract first page (index 0)")
        void firstPage() throws IOException {
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(testPdf, Set.of(0))) {
                assertEquals(1, doc.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("Extract middle page")
        void middlePage() throws IOException {
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(testPdf, Set.of(2))) {
                assertEquals(1, doc.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("Extract last page")
        void lastPage() throws IOException {
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(testPdf, Set.of(4))) {
                assertEquals(1, doc.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("Extract multiple non-contiguous pages")
        void nonContiguous() throws IOException {
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(testPdf, Set.of(0, 2, 4))) {
                assertEquals(3, doc.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("Extract contiguous pages")
        void contiguous() throws IOException {
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(testPdf, Set.of(1, 2, 3))) {
                assertEquals(3, doc.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("Keep all pages")
        void allPages() throws IOException {
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(testPdf, Set.of(0, 1, 2, 3, 4))) {
                assertEquals(5, doc.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("Single-page PDF")
        void singlePagePdf() throws IOException {
            File f = createBlankPdf(1);
            filesToCleanup.add(f);
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(f, Set.of(0))) {
                assertEquals(1, doc.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("Out-of-range index ignored")
        void outOfRange() throws IOException {
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(testPdf, Set.of(0, 99))) {
                assertEquals(1, doc.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("Throw on null pagesToKeep")
        void nullSet() {
            assertThrows(IllegalArgumentException.class,
                    () -> PdfSplitUtils.createDocumentWithPages(testPdf, null));
        }

        @Test
        @DisplayName("Throw on empty pagesToKeep")
        void emptySet() {
            assertThrows(IllegalArgumentException.class,
                    () -> PdfSplitUtils.createDocumentWithPages(testPdf, Set.of()));
        }

        @Test
        @DisplayName("Throw IOException for non-existent file")
        void missingFile() {
            assertThrows(IOException.class,
                    () -> PdfSplitUtils.createDocumentWithPages(new File("missing.pdf"), Set.of(0)));
        }
    }

    // =====================================================================
    //  createDocumentWithPageRange
    // =====================================================================
    @Nested
    @DisplayName("createDocumentWithPageRange")
    class CreateDocumentWithPageRange {

        @Test
        @DisplayName("[0,2) — first two pages")
        void firstTwo() throws IOException {
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPageRange(testPdf, 0, 2)) {
                assertEquals(2, doc.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("[2,5) — last three pages")
        void lastThree() throws IOException {
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPageRange(testPdf, 2, 5)) {
                assertEquals(3, doc.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("[0,1) — first page only")
        void firstOnly() throws IOException {
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPageRange(testPdf, 0, 1)) {
                assertEquals(1, doc.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("[4,5) — last page only")
        void lastOnly() throws IOException {
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPageRange(testPdf, 4, 5)) {
                assertEquals(1, doc.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("[0,5) — full range keeps all pages")
        void fullRange() throws IOException {
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPageRange(testPdf, 0, 5)) {
                assertEquals(5, doc.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("Single-page PDF [0,1)")
        void singlePagePdf() throws IOException {
            File f = createBlankPdf(1);
            filesToCleanup.add(f);
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPageRange(f, 0, 1)) {
                assertEquals(1, doc.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("Throw on negative startIndex")
        void negativeStart() {
            assertThrows(IllegalArgumentException.class,
                    () -> PdfSplitUtils.createDocumentWithPageRange(testPdf, -1, 3));
        }

        @Test
        @DisplayName("Throw when startIndex == endIndex")
        void emptyRange() {
            assertThrows(IllegalArgumentException.class,
                    () -> PdfSplitUtils.createDocumentWithPageRange(testPdf, 2, 2));
        }

        @Test
        @DisplayName("Throw when startIndex > endIndex")
        void invertedRange() {
            assertThrows(IllegalArgumentException.class,
                    () -> PdfSplitUtils.createDocumentWithPageRange(testPdf, 4, 2));
        }

        @Test
        @DisplayName("Throw when endIndex > totalPages")
        void exceedsPages() {
            assertThrows(IllegalArgumentException.class,
                    () -> PdfSplitUtils.createDocumentWithPageRange(testPdf, 0, 99));
        }

        @Test
        @DisplayName("Throw IOException for non-existent file")
        void missingFile() {
            assertThrows(IOException.class,
                    () -> PdfSplitUtils.createDocumentWithPageRange(new File("missing.pdf"), 0, 1));
        }
    }

    // =====================================================================
    //  Round-trip integrity
    // =====================================================================
    @Nested
    @DisplayName("Round-trip integrity")
    class RoundTrip {

        @Test
        @DisplayName("Save + reload preserves page count (range)")
        void range_pageCount() throws IOException {
            File out = tempFile("rt_range");
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPageRange(testPdf, 1, 3)) {
                doc.save(out);
            }
            try (PDDocument re = PDDocument.load(out)) {
                assertEquals(2, re.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("Save + reload preserves page count (set)")
        void set_pageCount() throws IOException {
            File out = tempFile("rt_set");
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(testPdf, Set.of(0, 4))) {
                doc.save(out);
            }
            try (PDDocument re = PDDocument.load(out)) {
                assertEquals(2, re.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("Save + reload preserves extractable text")
        void textPreserved() throws IOException {
            File textPdf = createPdfWithTextPages(3);
            filesToCleanup.add(textPdf);
            File out = tempFile("rt_text");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(textPdf, Set.of(1))) {
                doc.save(out);
            }
            try (PDDocument re = PDDocument.load(out)) {
                String text = new PDFTextStripper().getText(re);
                assertTrue(text.contains("Page 2"));
            }
        }

        @Test
        @DisplayName("Double save-reload cycle")
        void doubleCycle() throws IOException {
            File a = tempFile("rt_a");
            File b = tempFile("rt_b");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPageRange(testPdf, 0, 2)) {
                doc.save(a);
            }
            try (PDDocument re = PDDocument.load(a)) {
                re.save(b);
            }
            try (PDDocument re2 = PDDocument.load(b)) {
                assertEquals(2, re2.getNumberOfPages());
            }
        }
    }

    // =====================================================================
    //  Resource stripping — ALL resource types used
    // =====================================================================
    @Nested
    @DisplayName("Resource stripping — all types used")
    class AllResourceTypesUsed {

        @Test
        @DisplayName("Page using all 6 resource types should retain them all after split")
        void allTypesRetained() throws IOException {
            File pdf = createPdfWithAllResourceTypes();
            filesToCleanup.add(pdf);
            File out = tempFile("all_types");

            // Keep page 0 which uses Font, XObject, ExtGState, ColorSpace, Shading, Pattern
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(out);
            }

            try (PDDocument re = PDDocument.load(out)) {
                PDResources res = re.getPage(0).getResources();
                assertNotNull(res);
                assertTrue(res.getFontNames().iterator().hasNext(), "Font should be retained");
                assertTrue(res.getXObjectNames().iterator().hasNext(), "XObject should be retained");
                assertTrue(res.getExtGStateNames().iterator().hasNext(), "ExtGState should be retained");
                assertTrue(res.getColorSpaceNames().iterator().hasNext(), "ColorSpace should be retained");
                assertTrue(res.getShadingNames().iterator().hasNext(), "Shading should be retained");
                assertTrue(res.getPatternNames().iterator().hasNext(), "Pattern should be retained");
            }
        }

        @Test
        @DisplayName("ProcSet should be preserved when present")
        void procSetPreserved() throws IOException {
            File pdf = createPdfWithAllResourceTypes();
            filesToCleanup.add(pdf);
            File out = tempFile("procset");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(out);
            }

            try (PDDocument re = PDDocument.load(out)) {
                COSDictionary resDict = re.getPage(0).getResources().getCOSObject();
                assertNotNull(resDict.getDictionaryObject(COSName.PROC_SET),
                        "ProcSet should be preserved");
            }
        }
    }

    // =====================================================================
    //  Resource stripping — unused resources filtered out
    // =====================================================================
    @Nested
    @DisplayName("Resource stripping — unused filtered out")
    class UnusedResourcesFiltered {

        @Test
        @DisplayName("Page using only Font+XObject should NOT have ExtGState/ColorSpace/Shading/Pattern")
        void unusedTypesFiltered() throws IOException {
            File pdf = createTwoPagePdfWithSplitResources();
            filesToCleanup.add(pdf);
            File out = tempFile("unused_filtered");

            // Page 0 content uses only F1 (Tf) and Im1 (Do)
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(out);
            }

            try (PDDocument re = PDDocument.load(out)) {
                PDResources res = re.getPage(0).getResources();
                assertNotNull(res);
                assertTrue(res.getFontNames().iterator().hasNext(), "Font should be kept");
                assertTrue(res.getXObjectNames().iterator().hasNext(), "XObject should be kept");
                assertFalse(res.getExtGStateNames().iterator().hasNext(), "ExtGState should be filtered");
                assertFalse(res.getColorSpaceNames().iterator().hasNext(), "ColorSpace should be filtered");
                assertFalse(res.getShadingNames().iterator().hasNext(), "Shading should be filtered");
                assertFalse(res.getPatternNames().iterator().hasNext(), "Pattern should be filtered");
            }
        }

        @Test
        @DisplayName("Page using only ExtGState/CS/Shading/Pattern should NOT have Font/XObject")
        void reverseFilteredTypes() throws IOException {
            File pdf = createTwoPagePdfWithSplitResources();
            filesToCleanup.add(pdf);
            File out = tempFile("reverse_filtered");

            // Page 1 content uses GS1 (gs), CS1 (cs), Sh1 (sh), Pat1 (scn)
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(1))) {
                doc.save(out);
            }

            try (PDDocument re = PDDocument.load(out)) {
                PDResources res = re.getPage(0).getResources();
                assertNotNull(res);
                assertFalse(res.getFontNames().iterator().hasNext(), "Font should be filtered");
                assertFalse(res.getXObjectNames().iterator().hasNext(), "XObject should be filtered");
                assertTrue(res.getExtGStateNames().iterator().hasNext(), "ExtGState should be kept");
                assertTrue(res.getColorSpaceNames().iterator().hasNext(), "ColorSpace should be kept");
                assertTrue(res.getShadingNames().iterator().hasNext(), "Shading should be kept");
                assertTrue(res.getPatternNames().iterator().hasNext(), "Pattern should be kept");
            }
        }

        @Test
        @DisplayName("ProcSet absent should not cause errors")
        void noProcSet() throws IOException {
            File textPdf = createPdfWithTextPages(2);
            filesToCleanup.add(textPdf);
            File out = tempFile("no_procset");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(textPdf, Set.of(0))) {
                doc.save(out);
            }

            try (PDDocument re = PDDocument.load(out)) {
                assertEquals(1, re.getNumberOfPages());
            }
        }
    }

    // =====================================================================
    //  Resource stripping — null resource values from getters
    // =====================================================================
    @Nested
    @DisplayName("Resource stripping — null resource values")
    class NullResourceValues {

        @Test
        @DisplayName("Bogus font entry (not a COSDictionary) should be skipped via null check")
        void bogusFontEntry() throws IOException {
            File pdf = createPdfWithBogusResources();
            filesToCleanup.add(pdf);
            File out = tempFile("bogus_font");

            // Should not throw — bogus entries return null from getFont/getXObject and are skipped
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(out);
            }

            try (PDDocument re = PDDocument.load(out)) {
                assertEquals(1, re.getNumberOfPages());
                PDResources res = re.getPage(0).getResources();
                // The valid font should still be present
                assertTrue(res.getFontNames().iterator().hasNext(), "Valid font should remain");
            }
        }
    }

    // =====================================================================
    //  Resource stripping — content stream operators
    // =====================================================================
    @Nested
    @DisplayName("Resource stripping — content stream operators")
    class ContentStreamOperators {

        @Test
        @DisplayName("CS operator (non-stroking colorspace) should be recognized")
        void csOperator() throws IOException {
            File pdf = createPdfWithOperators("/CS1 CS\n");
            filesToCleanup.add(pdf);
            File out = tempFile("op_CS");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(out);
            }

            try (PDDocument re = PDDocument.load(out)) {
                assertTrue(re.getPage(0).getResources().getColorSpaceNames().iterator().hasNext(),
                        "CS operator should retain color space");
            }
        }

        @Test
        @DisplayName("SCN operator (stroking pattern color) should be recognized")
        void scnOperator() throws IOException {
            File pdf = createPdfWithOperators("0.5 /Pat1 SCN\n");
            filesToCleanup.add(pdf);
            File out = tempFile("op_SCN");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(out);
            }

            try (PDDocument re = PDDocument.load(out)) {
                assertTrue(re.getPage(0).getResources().getPatternNames().iterator().hasNext(),
                        "SCN operator should retain pattern");
            }
        }

        @Test
        @DisplayName("DP (marked-content point) operator should be recognized")
        void dpOperator() throws IOException {
            // DP takes a tag name and optionally a properties dict; we test name extraction
            File pdf = createPdfWithOperators("/Tag1 /Props1 DP\n");
            filesToCleanup.add(pdf);

            // Should not throw during resource stripping
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                assertNotNull(doc);
            }
        }

        @Test
        @DisplayName("BDC (begin marked-content) operator should be recognized")
        void bdcOperator() throws IOException {
            File pdf = createPdfWithOperators("/Tag1 /Props1 BDC\nEMC\n");
            filesToCleanup.add(pdf);

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                assertNotNull(doc);
            }
        }

        @Test
        @DisplayName("Non-resource operators should be ignored")
        void nonResourceOperators() throws IOException {
            // q, Q, m, l are not resource operators — they should not cause name extraction
            File pdf = createPdfWithOperators("q\n100 200 m\n300 400 l\nQ\n");
            filesToCleanup.add(pdf);

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                assertNotNull(doc);
            }
        }

        @Test
        @DisplayName("Backward walk limited to 4 operands — 5th operand is ignored")
        void backwardWalkLimit() throws IOException {
            // SCN with 5 operands: 0.1 0.2 0.3 0.4 /Pat1 SCN
            // The walk goes back max 4 positions, so it checks positions i-1 to i-4
            // The 5th operand (0.1) is beyond the limit and never checked
            File pdf = createPdfWithOperators("0.1 0.2 0.3 0.4 /Pat1 SCN\n");
            filesToCleanup.add(pdf);
            File out = tempFile("bw_limit");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(out);
            }

            try (PDDocument re = PDDocument.load(out)) {
                assertTrue(re.getPage(0).getResources().getPatternNames().iterator().hasNext(),
                        "Pattern should be retained (COSName within 4-operand window)");
            }
        }

        @Test
        @DisplayName("Backward walk: number operands before COSName should not block extraction")
        void backwardWalkNumbers() throws IOException {
            // Tf has: /FontName size Tf — parser walks back past the number to find the COSName
            File textPdf = createPdfWithTextPages(1);
            filesToCleanup.add(textPdf);
            File out = tempFile("bw_numbers");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(textPdf, Set.of(0))) {
                doc.save(out);
            }

            try (PDDocument re = PDDocument.load(out)) {
                assertTrue(re.getPage(0).getResources().getFontNames().iterator().hasNext(),
                        "Font name should be found despite number operand before it");
            }
        }
    }

    // =====================================================================
    //  stripUnusedResources — direct invocation
    // =====================================================================
    @Nested
    @DisplayName("stripUnusedResources (direct)")
    class StripDirect {

        @Test
        @DisplayName("Should not throw on blank pages (null content stream)")
        void blankPages() throws IOException {
            try (PDDocument doc = PDDocument.load(testPdf)) {
                assertDoesNotThrow(() -> PdfSplitUtils.stripUnusedResources(doc));
            }
        }

        @Test
        @DisplayName("Should not throw on text pages")
        void textPages() throws IOException {
            File f = createPdfWithTextPages(2);
            filesToCleanup.add(f);
            try (PDDocument doc = PDDocument.load(f)) {
                assertDoesNotThrow(() -> PdfSplitUtils.stripUnusedResources(doc));
            }
        }

        @Test
        @DisplayName("Should not throw on image pages")
        void imagePages() throws IOException {
            File f = createPdfWithDistinctImages(2);
            filesToCleanup.add(f);
            try (PDDocument doc = PDDocument.load(f)) {
                assertDoesNotThrow(() -> PdfSplitUtils.stripUnusedResources(doc));
            }
        }

        @Test
        @DisplayName("Should remove /Resources from page tree root")
        void removesFromRoot() throws IOException {
            File f = createPdfWithTextPages(2);
            filesToCleanup.add(f);
            try (PDDocument doc = PDDocument.load(f)) {
                PdfSplitUtils.stripUnusedResources(doc);
                assertNull(doc.getPages().getCOSObject().getDictionaryObject(COSName.RESOURCES));
            }
        }

        @Test
        @DisplayName("Each page should have its own /Resources after stripping")
        void pagesHaveOwnResources() throws IOException {
            File f = createPdfWithTextPages(2);
            filesToCleanup.add(f);
            try (PDDocument doc = PDDocument.load(f)) {
                PdfSplitUtils.stripUnusedResources(doc);
                for (PDPage page : doc.getPages()) {
                    assertNotNull(page.getCOSObject().getDictionaryObject(COSName.RESOURCES),
                            "Page should have its own /Resources");
                }
            }
        }

        @Test
        @DisplayName("Document should be saveable after stripping")
        void saveableAfterStrip() throws IOException {
            File f = createPdfWithTextPages(3);
            filesToCleanup.add(f);
            File out = tempFile("strip_save");

            try (PDDocument doc = PDDocument.load(f)) {
                PdfSplitUtils.stripUnusedResources(doc);
                doc.save(out);
            }
            try (PDDocument re = PDDocument.load(out)) {
                assertEquals(3, re.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("Page with null resources should be skipped")
        void nullResources() throws IOException {
            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage();
                doc.addPage(page);
                // Page has no resources and no content — null resources path
                assertDoesNotThrow(() -> PdfSplitUtils.stripUnusedResources(doc));
            }
        }

        @Test
        @DisplayName("Page with resources but empty content stream should not throw")
        void emptyContentStream() throws IOException {
            // Page loaded from file with text, then content stream replaced with empty bytes.
            // Parser yields zero tokens → usedNames is empty → resources get filtered.
            File textPdf = createPdfWithTextPages(1);
            filesToCleanup.add(textPdf);
            File out = tempFile("empty_cs");

            try (PDDocument doc = PDDocument.load(textPdf)) {
                PDPage page = doc.getPage(0);
                // Replace content stream with empty bytes
                PDStream emptyStream = new PDStream(doc);
                try (OutputStream os = emptyStream.createOutputStream()) {
                    // write nothing — parser finds zero tokens
                }
                page.getCOSObject().setItem(COSName.CONTENTS, emptyStream.getCOSObject());
                PdfSplitUtils.stripUnusedResources(doc);
                doc.save(out);
            }
            // Verify the output is a valid 1-page PDF
            try (PDDocument re = PDDocument.load(out)) {
                assertEquals(1, re.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("Corrupt content stream triggers fallback (keeps all resources)")
        void corruptContentStream() throws IOException {
            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage();
                doc.addPage(page);
                PDResources res = new PDResources();
                res.put(COSName.getPDFName("F1"), PDType1Font.HELVETICA);
                page.setResources(res);

                // Write an inline image with no closing EI marker — triggers IOException in parser
                PDStream stream = new PDStream(doc);
                try (OutputStream os = stream.createOutputStream()) {
                    // BI starts inline image data; ID begins data block
                    // Missing EI end-marker causes parse failure
                    String corrupt = "BI /W 1 /H 1 /BPC 8 /CS /G ID \u0000";
                    os.write(corrupt.getBytes(StandardCharsets.US_ASCII));
                }
                page.getCOSObject().setItem(COSName.CONTENTS, stream.getCOSObject());

                // Should not throw — catch block keeps all resources as fallback
                assertDoesNotThrow(() -> PdfSplitUtils.stripUnusedResources(doc));
                // Font should still be present (fallback kept everything)
                assertTrue(page.getResources().getFontNames().iterator().hasNext(),
                        "Fallback should preserve all resources when parsing fails");
            }
        }
    }

    // =====================================================================
    //  Inherited resources scenario
    // =====================================================================
    @Nested
    @DisplayName("Inherited resources")
    class InheritedResources {

        @Test
        @DisplayName("Inherited resources from page tree root should be resolved and filtered per-page")
        void inheritedFiltered() throws IOException {
            File pdf = createPdfWithInheritedResources();
            filesToCleanup.add(pdf);
            File out = tempFile("inherited");

            // Keep page 0 which uses F1 only (F2 is only used by page 1)
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(out);
            }

            try (PDDocument re = PDDocument.load(out)) {
                PDResources res = re.getPage(0).getResources();
                assertNotNull(res);
                // Count fonts — should be 1 (F1 only, not F2)
                int fontCount = 0;
                for (COSName ignored : res.getFontNames()) {
                    fontCount++;
                }
                assertEquals(1, fontCount, "Only the used font (F1) should remain");
            }
        }

        @Test
        @DisplayName("Page tree root /Resources should be cleared after split")
        void rootCleared() throws IOException {
            File pdf = createPdfWithInheritedResources();
            filesToCleanup.add(pdf);

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                assertNull(doc.getPages().getCOSObject().getDictionaryObject(COSName.RESOURCES));
            }
        }
    }

    // =====================================================================
    //  File size verification
    // =====================================================================
    @Nested
    @DisplayName("File size")
    class FileSize {

        @Test
        @DisplayName("Single-page split < original with distinct images")
        void singlePage_smaller() throws IOException {
            File pdf = createPdfWithDistinctImages(5);
            filesToCleanup.add(pdf);
            long original = pdf.length();

            File out = tempFile("size_single");
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(out);
            }
            assertTrue(out.length() < original,
                    String.format("Split %d bytes should be < original %d bytes", out.length(), original));
        }

        @Test
        @DisplayName("Half-range split < original")
        void halfRange_smaller() throws IOException {
            File pdf = createPdfWithDistinctImages(4);
            filesToCleanup.add(pdf);
            long original = pdf.length();

            File out = tempFile("size_half");
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPageRange(pdf, 0, 2)) {
                doc.save(out);
            }
            assertTrue(out.length() < original);
        }

        @Test
        @DisplayName("Sum of all individual splits < N × original")
        void sum_lessThanNTimesOriginal() throws IOException {
            int n = 4;
            File pdf = createPdfWithDistinctImages(n);
            filesToCleanup.add(pdf);
            long original = pdf.length();

            long totalSplit = 0;
            for (int i = 0; i < n; i++) {
                File out = tempFile("size_sum_" + i);
                try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(i))) {
                    doc.save(out);
                }
                totalSplit += out.length();
            }
            assertTrue(totalSplit < (long) n * original);
        }

        @Test
        @DisplayName("No single split exceeds original size")
        void noSplit_exceedsOriginal() throws IOException {
            int n = 4;
            File pdf = createPdfWithDistinctImages(n);
            filesToCleanup.add(pdf);
            long original = pdf.length();

            for (int i = 0; i < n; i++) {
                File out = tempFile("size_nox_" + i);
                try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(i))) {
                    doc.save(out);
                }
                assertTrue(out.length() <= original);
            }
        }
    }

    // =====================================================================
    //  Text content correctness
    // =====================================================================
    @Nested
    @DisplayName("Text content")
    class TextContent {

        @Test
        @DisplayName("Kept page text is extractable")
        void keptPageText() throws IOException {
            File pdf = createPdfWithTextPages(5);
            filesToCleanup.add(pdf);
            File out = tempFile("txt_kept");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(2))) {
                doc.save(out);
            }
            try (PDDocument re = PDDocument.load(out)) {
                assertTrue(new PDFTextStripper().getText(re).contains("Page 3"));
            }
        }

        @Test
        @DisplayName("Excluded page text NOT present")
        void excludedPageText() throws IOException {
            File pdf = createPdfWithTextPages(5);
            filesToCleanup.add(pdf);
            File out = tempFile("txt_excl");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(out);
            }
            try (PDDocument re = PDDocument.load(out)) {
                String text = new PDFTextStripper().getText(re);
                assertTrue(text.contains("Page 1"));
                assertFalse(text.contains("Page 2"));
                assertFalse(text.contains("Page 3"));
            }
        }

        @Test
        @DisplayName("Multiple kept pages each have correct text")
        void multipleKeptPages() throws IOException {
            File pdf = createPdfWithTextPages(5);
            filesToCleanup.add(pdf);
            File out = tempFile("txt_multi");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(1, 3))) {
                doc.save(out);
            }
            try (PDDocument re = PDDocument.load(out)) {
                PDFTextStripper s = new PDFTextStripper();
                s.setStartPage(1);
                s.setEndPage(1);
                assertTrue(s.getText(re).contains("Page 2"));
                s.setStartPage(2);
                s.setEndPage(2);
                assertTrue(s.getText(re).contains("Page 4"));
            }
        }
    }

    // =====================================================================
    //  Edge cases
    // =====================================================================
    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Large PDF (50 pages) range extraction")
        void largePdf() throws IOException {
            File f = createBlankPdf(50);
            filesToCleanup.add(f);
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPageRange(f, 20, 30)) {
                assertEquals(10, doc.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("createDocumentWithPages and createDocumentWithPageRange produce same result")
        void equivalentApis() throws IOException {
            File a = tempFile("eq_pages");
            File b = tempFile("eq_range");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(testPdf, Set.of(1, 2, 3))) {
                doc.save(a);
            }
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPageRange(testPdf, 1, 4)) {
                doc.save(b);
            }
            try (PDDocument da = PDDocument.load(a); PDDocument db = PDDocument.load(b)) {
                assertEquals(da.getNumberOfPages(), db.getNumberOfPages());
            }
        }

        @Test
        @DisplayName("Two independent splits from same source")
        void independentSplits() throws IOException {
            File pdf = createPdfWithTextPages(4);
            filesToCleanup.add(pdf);
            File a = tempFile("ind_a");
            File b = tempFile("ind_b");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(a);
            }
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(3))) {
                doc.save(b);
            }

            try (PDDocument da = PDDocument.load(a); PDDocument db = PDDocument.load(b)) {
                PDFTextStripper s = new PDFTextStripper();
                assertTrue(s.getText(da).contains("Page 1"));
                assertTrue(s.getText(db).contains("Page 4"));
            }
        }

        @Test
        @DisplayName("Blank page (null content stream) should not cause errors")
        void blankPageSplit() throws IOException {
            File out = tempFile("blank_split");
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(testPdf, Set.of(0))) {
                doc.save(out);
            }
            try (PDDocument re = PDDocument.load(out)) {
                assertEquals(1, re.getNumberOfPages());
            }
        }
    }

    // =====================================================================
    //  Document-level overhead stripping
    // =====================================================================
    @Nested
    @DisplayName("Document-level overhead stripping")
    class DocumentLevelOverhead {

        @Test
        @DisplayName("OutputIntents should be removed after split")
        void outputIntentsRemoved() throws IOException {
            File pdf = createPdfWithDocumentLevelObjects();
            filesToCleanup.add(pdf);
            File out = tempFile("doc_oi");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(out);
            }
            try (PDDocument re = PDDocument.load(out)) {
                COSDictionary catalog = re.getDocumentCatalog().getCOSObject();
                assertNull(catalog.getDictionaryObject(COSName.OUTPUT_INTENTS),
                        "OutputIntents should be stripped from split");
            }
        }

        @Test
        @DisplayName("StructTreeRoot should be removed after split")
        void structTreeRootRemoved() throws IOException {
            File pdf = createPdfWithDocumentLevelObjects();
            filesToCleanup.add(pdf);
            File out = tempFile("doc_str");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(out);
            }
            try (PDDocument re = PDDocument.load(out)) {
                COSDictionary catalog = re.getDocumentCatalog().getCOSObject();
                assertNull(catalog.getDictionaryObject(COSName.STRUCT_TREE_ROOT),
                        "StructTreeRoot should be stripped from split");
            }
        }

        @Test
        @DisplayName("AcroForm should be removed after split")
        void acroFormRemoved() throws IOException {
            File pdf = createPdfWithDocumentLevelObjects();
            filesToCleanup.add(pdf);
            File out = tempFile("doc_af");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(out);
            }
            try (PDDocument re = PDDocument.load(out)) {
                COSDictionary catalog = re.getDocumentCatalog().getCOSObject();
                assertNull(catalog.getDictionaryObject(COSName.ACRO_FORM),
                        "AcroForm should be stripped from split");
            }
        }

        @Test
        @DisplayName("PDF with document-level objects should produce smaller splits")
        void smallerSplitsWithDocLevelObjects() throws IOException {
            File pdf = createPdfWithDocumentLevelObjects();
            filesToCleanup.add(pdf);
            long originalSize = pdf.length();

            File out = tempFile("doc_size");
            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(out);
            }
            assertTrue(out.length() < originalSize,
                    String.format("Split (%d bytes) should be smaller than original (%d bytes)",
                            out.length(), originalSize));
        }

        @Test
        @DisplayName("All catalog-level entries should be removed after split")
        void allCatalogEntriesRemoved() throws IOException {
            File pdf = createPdfWithDocumentLevelObjects();
            filesToCleanup.add(pdf);
            File out = tempFile("doc_all");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(out);
            }
            try (PDDocument re = PDDocument.load(out)) {
                COSDictionary catalog = re.getDocumentCatalog().getCOSObject();
                assertNull(catalog.getDictionaryObject(COSName.OUTPUT_INTENTS), "OutputIntents");
                assertNull(catalog.getDictionaryObject(COSName.STRUCT_TREE_ROOT), "StructTreeRoot");
                assertNull(catalog.getDictionaryObject(COSName.NAMES), "Names");
                assertNull(catalog.getDictionaryObject(COSName.OUTLINES), "Outlines");
                assertNull(catalog.getDictionaryObject(COSName.ACRO_FORM), "AcroForm");
                assertNull(catalog.getDictionaryObject(COSName.METADATA), "Metadata");
                assertNull(catalog.getDictionaryObject(COSName.getPDFName("MarkInfo")), "MarkInfo");
                assertNull(catalog.getDictionaryObject(COSName.THREADS), "Threads");
                assertNull(catalog.getDictionaryObject(COSName.OPEN_ACTION), "OpenAction");
            }
        }

        @Test
        @DisplayName("Page content should still be intact after document-level stripping")
        void pageContentIntact() throws IOException {
            File pdf = createPdfWithDocumentLevelObjects();
            filesToCleanup.add(pdf);
            File out = tempFile("doc_txt");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPages(pdf, Set.of(0))) {
                doc.save(out);
            }
            try (PDDocument re = PDDocument.load(out)) {
                assertEquals(1, re.getNumberOfPages());
                String text = new PDFTextStripper().getText(re);
                assertTrue(text.contains("Page 1"), "Page text should be preserved");
            }
        }

        @Test
        @DisplayName("createDocumentWithPageRange should also strip document-level objects")
        void pageRangeAlsoStrips() throws IOException {
            File pdf = createPdfWithDocumentLevelObjects();
            filesToCleanup.add(pdf);
            File out = tempFile("doc_range");

            try (PDDocument doc = PdfSplitUtils.createDocumentWithPageRange(pdf, 0, 1)) {
                doc.save(out);
            }
            try (PDDocument re = PDDocument.load(out)) {
                COSDictionary catalog = re.getDocumentCatalog().getCOSObject();
                assertNull(catalog.getDictionaryObject(COSName.OUTPUT_INTENTS),
                        "OutputIntents should be stripped via page-range API too");
            }
        }
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    private File tempFile(String prefix) {
        File f = new File(prefix + "_test.pdf");
        filesToCleanup.add(f);
        return f;
    }

    /** Blank pages — no content stream, no resources */
    private File createBlankPdf(int pages) throws IOException {
        File file = File.createTempFile("blank_", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pages; i++) {
                doc.addPage(new PDPage());
            }
            doc.save(file);
        }
        return file;
    }

    /** Each page renders text with Helvetica (exercises Tf operator) */
    private File createPdfWithTextPages(int count) throws IOException {
        File file = File.createTempFile("text_", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < count; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(50, 700);
                    cs.showText("Page " + (i + 1) + " content");
                    cs.endText();
                }
            }
            doc.save(file);
        }
        return file;
    }

    /** Each page has a distinct image (exercises Do operator) */
    private File createPdfWithDistinctImages(int count) throws IOException {
        File file = File.createTempFile("images_", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < count; i++) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setColor(new Color((i * 60) % 256, 100, 150));
                g.fillRect(0, 0, 200, 200);
                g.dispose();

                File imgFile = File.createTempFile("img_", ".png");
                ImageIO.write(img, "png", imgFile);
                filesToCleanup.add(imgFile);

                PDImageXObject pdImg = PDImageXObject.createFromFile(imgFile.getAbsolutePath(), doc);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(pdImg, 50, 500, 200, 200);
                }
            }
            doc.save(file);
        }
        return file;
    }

    /**
     * Creates a single-page PDF whose resources include ALL 6 types
     * (Font, XObject, ExtGState, ColorSpace, Shading, Pattern + ProcSet)
     * and whose content stream references every one of them.
     * This exercises every loop in buildFilteredResources and every
     * operator in RESOURCE_OPERATORS.
     */
    private File createPdfWithAllResourceTypes() throws IOException {
        File file = File.createTempFile("all_res_", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDResources resources = new PDResources();

            // 1. Font
            resources.put(COSName.getPDFName("F1"), PDType1Font.HELVETICA);

            // 2. Image XObject
            BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            File imgFile = File.createTempFile("tiny_", ".png");
            ImageIO.write(img, "png", imgFile);
            filesToCleanup.add(imgFile);
            PDImageXObject pdImg = PDImageXObject.createFromFile(imgFile.getAbsolutePath(), doc);
            resources.put(COSName.getPDFName("Im1"), pdImg);

            // 3. ExtGState
            PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
            gs.setNonStrokingAlphaConstant(0.5f);
            resources.put(COSName.getPDFName("GS1"), gs);

            // 4. ColorSpace (named device RGB)
            resources.put(COSName.getPDFName("CS1"), PDDeviceRGB.INSTANCE);

            // 5. Shading (axial type 2)
            COSDictionary shadDict = new COSDictionary();
            shadDict.setInt(COSName.SHADING_TYPE, 2);
            shadDict.setItem(COSName.COLORSPACE, COSName.DEVICERGB);
            COSArray coords = new COSArray();
            coords.add(new COSFloat(0f));
            coords.add(new COSFloat(0f));
            coords.add(new COSFloat(1f));
            coords.add(new COSFloat(1f));
            shadDict.setItem(COSName.COORDS, coords);
            resources.put(COSName.getPDFName("Sh1"), PDShading.create(shadDict));

            // 6. Pattern (tiling)
            PDTilingPattern pattern = new PDTilingPattern();
            pattern.setPaintType(1);
            pattern.setTilingType(1);
            pattern.setBBox(new PDRectangle(0, 0, 10, 10));
            pattern.setXStep(10);
            pattern.setYStep(10);
            resources.put(COSName.getPDFName("Pat1"), pattern);

            // 7. ProcSet (legacy)
            COSArray procSet = new COSArray();
            procSet.add(COSName.getPDFName("PDF"));
            procSet.add(COSName.getPDFName("Text"));
            resources.getCOSObject().setItem(COSName.PROC_SET, procSet);

            page.setResources(resources);

            // Content stream using all operators:
            //   gs → ExtGState, cs → ColorSpace, Tf → Font, Do → XObject, sh → Shading, scn → Pattern
            String content = "q\n"
                    + "/GS1 gs\n"
                    + "/CS1 cs\n"
                    + "BT /F1 12 Tf (Hello) Tj ET\n"
                    + "/Im1 Do\n"
                    + "/Sh1 sh\n"
                    + "0.5 /Pat1 scn\n"
                    + "Q\n";
            writeRawContent(doc, page, content);
            doc.save(file);
        }
        return file;
    }

    /**
     * 2-page PDF where BOTH pages have all 6 resource types, but:
     *   Page 0 content uses ONLY Font (Tf) + XObject (Do)
     *   Page 1 content uses ONLY ExtGState (gs) + ColorSpace (cs) + Shading (sh) + Pattern (scn)
     * This lets us test that unused resources are filtered out per page.
     */
    private File createTwoPagePdfWithSplitResources() throws IOException {
        File file = File.createTempFile("split_res_", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            // Build shared resources
            PDResources resources = buildAllResources(doc);

            // Page 0: uses only F1 (Tf) and Im1 (Do)
            PDPage page0 = new PDPage(PDRectangle.A4);
            doc.addPage(page0);
            page0.setResources(cloneResources(resources));
            writeRawContent(doc, page0, "BT /F1 12 Tf (Page1) Tj ET\n/Im1 Do\n");

            // Page 1: uses only GS1 (gs), CS1 (cs), Sh1 (sh), Pat1 (scn)
            PDPage page1 = new PDPage(PDRectangle.A4);
            doc.addPage(page1);
            page1.setResources(cloneResources(resources));
            writeRawContent(doc, page1, "/GS1 gs\n/CS1 cs\n/Sh1 sh\n0.5 /Pat1 scn\n");

            doc.save(file);
        }
        return file;
    }

    /**
     * Creates a PDF with bogus resource entries that will return null from
     * PDResources getters (covers the `if (font != null)` false branch).
     */
    private File createPdfWithBogusResources() throws IOException {
        File file = File.createTempFile("bogus_res_", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            PDResources resources = new PDResources();
            // Valid font
            resources.put(COSName.getPDFName("F1"), PDType1Font.HELVETICA);

            // Bogus font entry: COSName instead of COSDictionary → getFont returns null
            COSDictionary fontDict = (COSDictionary) resources.getCOSObject()
                    .getDictionaryObject(COSName.FONT);
            fontDict.setItem(COSName.getPDFName("FBogus"), COSName.getPDFName("NotAFont"));

            page.setResources(resources);

            // Content stream references both valid and bogus font names (no XObject reference)
            writeRawContent(doc, page, "BT /F1 12 Tf /FBogus 10 Tf (text) Tj ET\n");
            doc.save(file);
        }
        return file;
    }

    /**
     * Creates a PDF where resources are set on the page tree ROOT (inherited)
     * rather than on individual pages. Page 0 uses F1, page 1 uses F2.
     */
    private File createPdfWithInheritedResources() throws IOException {
        File file = File.createTempFile("inherited_", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page0 = new PDPage();
            doc.addPage(page0);
            PDPage page1 = new PDPage();
            doc.addPage(page1);

            // Set resources on the PAGE TREE ROOT (inherited by all pages)
            PDResources shared = new PDResources();
            shared.put(COSName.getPDFName("F1"), PDType1Font.HELVETICA);
            shared.put(COSName.getPDFName("F2"), PDType1Font.COURIER);
            doc.getPages().getCOSObject().setItem(COSName.RESOURCES, shared.getCOSObject());

            // Remove direct resources from pages so they inherit
            page0.getCOSObject().removeItem(COSName.RESOURCES);
            page1.getCOSObject().removeItem(COSName.RESOURCES);

            // Page 0 uses F1 only
            writeRawContent(doc, page0, "BT /F1 12 Tf (Page1) Tj ET\n");
            // Page 1 uses F2 only
            writeRawContent(doc, page1, "BT /F2 10 Tf (Page2) Tj ET\n");

            doc.save(file);
        }
        return file;
    }

    /**
     * Creates a single-page PDF with custom operators in its content stream.
     * The page has all resource types so that operator-matched names can be filtered.
     */
    private File createPdfWithOperators(String contentStreamOps) throws IOException {
        File file = File.createTempFile("ops_", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            page.setResources(buildAllResources(doc));
            writeRawContent(doc, page, contentStreamOps);
            doc.save(file);
        }
        return file;
    }

    /** Builds a PDResources containing all 6 resource types. */
    private PDResources buildAllResources(PDDocument doc) throws IOException {
        PDResources res = new PDResources();

        res.put(COSName.getPDFName("F1"), PDType1Font.HELVETICA);

        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        File imgFile = File.createTempFile("tiny_", ".png");
        ImageIO.write(img, "png", imgFile);
        filesToCleanup.add(imgFile);
        res.put(COSName.getPDFName("Im1"), PDImageXObject.createFromFile(imgFile.getAbsolutePath(), doc));

        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        gs.setNonStrokingAlphaConstant(0.5f);
        res.put(COSName.getPDFName("GS1"), gs);

        res.put(COSName.getPDFName("CS1"), PDDeviceRGB.INSTANCE);

        COSDictionary shadDict = new COSDictionary();
        shadDict.setInt(COSName.SHADING_TYPE, 2);
        shadDict.setItem(COSName.COLORSPACE, COSName.DEVICERGB);
        COSArray coords = new COSArray();
        coords.add(new COSFloat(0f));
        coords.add(new COSFloat(0f));
        coords.add(new COSFloat(1f));
        coords.add(new COSFloat(1f));
        shadDict.setItem(COSName.COORDS, coords);
        res.put(COSName.getPDFName("Sh1"), PDShading.create(shadDict));

        PDTilingPattern pat = new PDTilingPattern();
        pat.setPaintType(1);
        pat.setTilingType(1);
        pat.setBBox(new PDRectangle(0, 0, 10, 10));
        pat.setXStep(10);
        pat.setYStep(10);
        res.put(COSName.getPDFName("Pat1"), pat);

        return res;
    }

    /** Shallow-clones a PDResources by wrapping a copy of its COS dictionary. */
    private PDResources cloneResources(PDResources src) {
        return new PDResources(new COSDictionary(src.getCOSObject()));
    }

    // =====================================================================
    //  computeContentHash / digestCOSTree — 100 % branch coverage
    //
    //  Both methods are private; every branch is exercised through the
    //  public deduplicateResources() entry point.
    //
    //  Strategy: put specific COS objects as EXT_G_STATE (or XOBJECT)
    //  resource entries, call deduplicateResources(), then assert whether
    //  the two entries were collapsed to the same Java object (deduplicated)
    //  or remained distinct (not deduplicated / hash returned null).
    // =====================================================================
    @Nested
    @DisplayName("computeContentHash and digestCOSTree — full branch coverage")
    class ContentHashCoverage {

        // -----------------------------------------------------------------
        //  Helpers
        // -----------------------------------------------------------------

        /**
         * Adds a page whose EXT_G_STATE category dict contains one entry
         * named {@code "E1"} with the given value.
         *
         * @return the EXT_G_STATE COSDictionary (so callers can assert on it after dedup)
         */
        private COSDictionary addGsEntry(PDDocument doc, COSBase value) {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDResources res = new PDResources();
            COSDictionary gsDict = new COSDictionary();
            gsDict.setItem(COSName.getPDFName("E1"), value);
            res.getCOSObject().setItem(COSName.EXT_G_STATE, gsDict);
            page.setResources(res);
            return gsDict;
        }

        /**
         * Adds a page whose XOBJECT category dict contains one entry
         * named {@code "X1"} with the given value.
         *
         * @return the XOBJECT COSDictionary
         */
        private COSDictionary addXobjEntry(PDDocument doc, COSBase value) {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDResources res = new PDResources();
            COSDictionary xobjDict = new COSDictionary();
            xobjDict.setItem(COSName.getPDFName("X1"), value);
            res.getCOSObject().setItem(COSName.XOBJECT, xobjDict);
            page.setResources(res);
            return xobjDict;
        }

        /** Returns the resolved value of {@code "E1"} in the given EXT_G_STATE dict. */
        private COSBase entry(COSDictionary gsDict) {
            return gsDict.getDictionaryObject(COSName.getPDFName("E1"));
        }

        /** Returns the resolved value of {@code "X1"} in the given XOBJECT dict. */
        private COSBase xentry(COSDictionary xobjDict) {
            return xobjDict.getDictionaryObject(COSName.getPDFName("X1"));
        }

        // =================================================================
        //  deduplicateResources guard conditions
        // =================================================================

        @Test
        @DisplayName("guard: page with null resources is skipped without error")
        void guard_pageWithNullResources_isSkipped() throws IOException {
            try (PDDocument doc = new PDDocument()) {
                doc.addPage(new PDPage()); // no resources set → null
                assertDoesNotThrow(() -> PdfSplitUtils.deduplicateResources(doc));
            }
        }

        @Test
        @DisplayName("guard: category value that is not a COSDictionary (e.g. COSName) is skipped")
        void guard_categoryNotDictionary_isSkipped() throws IOException {
            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage();
                doc.addPage(page);
                PDResources res = new PDResources();
                // EXT_G_STATE → COSName, not a COSDictionary → condition !(catBase instanceof COSDictionary) is true → skip
                res.getCOSObject().setItem(COSName.EXT_G_STATE, COSName.getPDFName("NotADict"));
                page.setResources(res);
                assertDoesNotThrow(() -> PdfSplitUtils.deduplicateResources(doc));
            }
        }

        @Test
        @DisplayName("guard: category value that IS a COSStream (subclass of COSDictionary) is skipped")
        void guard_categoryIsStream_isSkipped() throws IOException {
            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage();
                doc.addPage(page);
                PDResources res = new PDResources();
                // COSStream IS a COSDictionary but the guard treats it as non-category
                COSStream catStream = new COSStream();
                try (OutputStream os = catStream.createOutputStream()) {
                    os.write("data".getBytes(StandardCharsets.UTF_8));
                }
                res.getCOSObject().setItem(COSName.EXT_G_STATE, catStream);
                page.setResources(res);
                assertDoesNotThrow(() -> PdfSplitUtils.deduplicateResources(doc));
            }
        }

        @Test
        @DisplayName("guard: resource entry whose getDictionaryObject resolves to null is skipped")
        void guard_resourceEntryResolvesToNull_isSkipped() throws IOException {
            // COSObject(null) stored as a resource entry.
            // catDict.getDictionaryObject(name) resolves the COSObject → returns null → `if (obj == null) continue`
            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage();
                doc.addPage(page);
                PDResources res = new PDResources();
                COSDictionary gsDict = new COSDictionary();
                gsDict.setItem(COSName.getPDFName("E1"), new COSObject(null));
                res.getCOSObject().setItem(COSName.EXT_G_STATE, gsDict);
                page.setResources(res);
                assertDoesNotThrow(() -> PdfSplitUtils.deduplicateResources(doc));
            }
        }

        @Test
        @DisplayName("guard: resource that is already the canonical object is not replaced with itself")
        void guard_sameObjectAlreadyCanonical_notReplacedWithItself() throws IOException {
            // Both pages share the exact same Java object.
            // existing != null but existing == obj → else branch (put again, no setItem called)
            COSDictionary sharedGs = new COSDictionary();
            sharedGs.setFloat(COSName.CA, 0.5f);

            try (PDDocument doc = new PDDocument()) {
                COSDictionary gs1 = addGsEntry(doc, sharedGs);
                COSDictionary gs2 = addGsEntry(doc, sharedGs);

                PdfSplitUtils.deduplicateResources(doc);

                assertSame(sharedGs, entry(gs1));
                assertSame(sharedGs, entry(gs2));
            }
        }

        // =================================================================
        //  computeContentHash — happy path returns non-null; catch returns null
        // =================================================================

        @Test
        @DisplayName("computeContentHash: identical COSDictionary objects → same hash → deduplicated")
        void computeHash_happyPath_identicalDicts_deduplicated() throws IOException {
            COSDictionary d1 = new COSDictionary();
            d1.setFloat(COSName.CA, 0.5f);
            COSDictionary d2 = new COSDictionary();
            d2.setFloat(COSName.CA, 0.5f);

            try (PDDocument doc = new PDDocument()) {
                COSDictionary gs1 = addGsEntry(doc, d1);
                COSDictionary gs2 = addGsEntry(doc, d2);

                PdfSplitUtils.deduplicateResources(doc);

                assertSame(entry(gs1), entry(gs2),
                        "Identical COSDictionaries should share one canonical object after dedup");
            }
        }

        @Test
        @DisplayName("computeContentHash: IOException from createRawInputStream → null hash → no dedup")
        void computeHash_ioException_returnsNull_noDedup() throws IOException {
            // Anonymous subclass overrides createRawInputStream to throw,
            // causing digestCOSTree to propagate IOException, caught by computeContentHash → returns null.
            try (PDDocument doc = new PDDocument()) {
                COSStream broken1 = new COSStream() {
                    @Override
                    public java.io.InputStream createRawInputStream() throws IOException {
                        throw new IOException("simulated read error");
                    }
                };
                COSStream broken2 = new COSStream() {
                    @Override
                    public java.io.InputStream createRawInputStream() throws IOException {
                        throw new IOException("simulated read error");
                    }
                };

                COSDictionary x1 = addXobjEntry(doc, broken1);
                COSDictionary x2 = addXobjEntry(doc, broken2);

                assertDoesNotThrow(() -> PdfSplitUtils.deduplicateResources(doc));

                // hash was null for both → no deduplication → still different objects
                assertNotSame(xentry(x1), xentry(x2),
                        "Unhashable resources must not be deduplicated");
            }
        }

        // =================================================================
        //  digestCOSTree — null obj and depth limit
        // =================================================================

        @Test
        @DisplayName("digestCOSTree: depth > MAX_HASH_DEPTH(12) → md.update(0) and return (no StackOverflow)")
        void digestCOSTree_depthLimitExceeded_handledGracefully() throws IOException {
            // Build two structurally identical 13-level deep dictionaries.
            // The leaf at depth 13 triggers `depth > MAX_HASH_DEPTH` → same sentinel byte → same hash → dedup.
            try (PDDocument doc = new PDDocument()) {
                COSDictionary gs1 = addGsEntry(doc, buildDeepDict(13));
                COSDictionary gs2 = addGsEntry(doc, buildDeepDict(13));

                assertDoesNotThrow(() -> PdfSplitUtils.deduplicateResources(doc));
                assertSame(entry(gs1), entry(gs2),
                        "Identically deep structures should be deduplicated");
            }
        }

        /** Builds a COSDictionary nested {@code levels} deep. */
        private COSDictionary buildDeepDict(int levels) {
            COSDictionary current = new COSDictionary();
            current.setItem(COSName.getPDFName("v"), COSBoolean.TRUE);
            for (int i = 0; i < levels; i++) {
                COSDictionary parent = new COSDictionary();
                parent.setItem(COSName.getPDFName("c"), current);
                current = parent;
            }
            return current;
        }

        @Test
        @DisplayName("digestCOSTree: COSObject with null inner (inside COSArray) → md.update(0) and return")
        void digestCOSTree_cosObjectNullInner_handledGracefully() throws IOException {
            // COSArray.get(i) returns raw COSBase (no resolution).
            // COSObject(null).getObject() == null → null branch fires.
            COSArray arr1 = new COSArray();
            arr1.add(new COSObject(null));
            COSArray arr2 = new COSArray();
            arr2.add(new COSObject(null));

            try (PDDocument doc = new PDDocument()) {
                COSDictionary gs1 = addGsEntry(doc, arr1);
                COSDictionary gs2 = addGsEntry(doc, arr2);

                assertDoesNotThrow(() -> PdfSplitUtils.deduplicateResources(doc));
                // Both produce identical byte sequences → same hash → deduplicated
                assertSame(entry(gs1), entry(gs2));
            }
        }

        // =================================================================
        //  digestCOSTree — COSObject resolution (non-null inner)
        // =================================================================

        @Test
        @DisplayName("digestCOSTree: COSObject with non-null inner (inside COSArray) is resolved then hashed")
        void digestCOSTree_cosObjectNonNullInner_resolvedAndHashed() throws IOException {
            // COSArray.get(0) → COSObject → resolved to COSName("foo") → COSName branch
            COSArray arr1 = new COSArray();
            arr1.add(new COSObject(COSName.getPDFName("foo")));
            COSArray arr2 = new COSArray();
            arr2.add(new COSObject(COSName.getPDFName("foo")));

            try (PDDocument doc = new PDDocument()) {
                COSDictionary gs1 = addGsEntry(doc, arr1);
                COSDictionary gs2 = addGsEntry(doc, arr2);

                assertDoesNotThrow(() -> PdfSplitUtils.deduplicateResources(doc));
                assertSame(entry(gs1), entry(gs2));
            }
        }

        // =================================================================
        //  digestCOSTree — cycle detection
        // =================================================================

        @Test
        @DisplayName("digestCOSTree: self-referencing COSDictionary → cycle detected → md.update(0xFF)")
        void digestCOSTree_selfReferencingDict_cycleDetected() throws IOException {
            // selfRef1 and selfRef2 are structurally identical self-referencing dicts.
            // Both produce the same byte sequence → same hash → deduplicated.
            COSDictionary selfRef1 = new COSDictionary();
            selfRef1.setItem(COSName.getPDFName("s"), selfRef1);
            COSDictionary selfRef2 = new COSDictionary();
            selfRef2.setItem(COSName.getPDFName("s"), selfRef2);

            try (PDDocument doc = new PDDocument()) {
                COSDictionary gs1 = addGsEntry(doc, selfRef1);
                COSDictionary gs2 = addGsEntry(doc, selfRef2);

                assertDoesNotThrow(() -> PdfSplitUtils.deduplicateResources(doc));
                assertSame(entry(gs1), entry(gs2),
                        "Identical self-referencing dicts should be deduplicated");
            }
        }

        @Test
        @DisplayName("digestCOSTree: self-referencing COSArray → cycle detected → md.update(0xFF)")
        void digestCOSTree_selfReferencingArray_cycleDetected() throws IOException {
            COSArray selfArr1 = new COSArray();
            selfArr1.add(selfArr1);
            COSArray selfArr2 = new COSArray();
            selfArr2.add(selfArr2);

            try (PDDocument doc = new PDDocument()) {
                COSDictionary gs1 = addGsEntry(doc, selfArr1);
                COSDictionary gs2 = addGsEntry(doc, selfArr2);

                assertDoesNotThrow(() -> PdfSplitUtils.deduplicateResources(doc));
                assertSame(entry(gs1), entry(gs2),
                        "Identical self-referencing arrays should be deduplicated");
            }
        }

        // =================================================================
        //  digestCOSTree — type dispatch: every instanceof branch
        // =================================================================

        @Test
        @DisplayName("digestCOSTree: COSStream → 'S' marker + raw bytes + dict entries")
        void digestCOSTree_cosStream_hashedAndDeduplicated() throws IOException {
            byte[] data = "stream payload".getBytes(StandardCharsets.UTF_8);

            try (PDDocument doc = new PDDocument()) {
                COSStream s1 = new COSStream();
                try (OutputStream os = s1.createOutputStream()) { os.write(data); }
                s1.setItem(COSName.getPDFName("Info"), COSName.getPDFName("A"));

                COSStream s2 = new COSStream();
                try (OutputStream os = s2.createOutputStream()) { os.write(data); }
                s2.setItem(COSName.getPDFName("Info"), COSName.getPDFName("A"));

                COSDictionary x1 = addXobjEntry(doc, s1);
                COSDictionary x2 = addXobjEntry(doc, s2);

                PdfSplitUtils.deduplicateResources(doc);

                assertSame(xentry(x1), xentry(x2),
                        "Identical COSStreams should be deduplicated");
            }
        }

        @Test
        @DisplayName("digestCOSTree: plain COSDictionary (non-stream) → 'D' marker + sorted entries")
        void digestCOSTree_cosDictionary_hashedAndDeduplicated() throws IOException {
            COSDictionary d1 = new COSDictionary();
            d1.setInt(COSName.getPDFName("k"), 42);
            COSDictionary d2 = new COSDictionary();
            d2.setInt(COSName.getPDFName("k"), 42);

            try (PDDocument doc = new PDDocument()) {
                COSDictionary gs1 = addGsEntry(doc, d1);
                COSDictionary gs2 = addGsEntry(doc, d2);

                PdfSplitUtils.deduplicateResources(doc);

                assertSame(entry(gs1), entry(gs2));
            }
        }

        @Test
        @DisplayName("digestCOSTree: COSArray → 'A' marker + recursive elements")
        void digestCOSTree_cosArray_hashedAndDeduplicated() throws IOException {
            COSArray arr1 = new COSArray();
            arr1.add(COSName.getPDFName("DeviceRGB"));
            arr1.add(COSName.getPDFName("Indexed"));
            COSArray arr2 = new COSArray();
            arr2.add(COSName.getPDFName("DeviceRGB"));
            arr2.add(COSName.getPDFName("Indexed"));

            try (PDDocument doc = new PDDocument()) {
                COSDictionary gs1 = addGsEntry(doc, arr1);
                COSDictionary gs2 = addGsEntry(doc, arr2);

                PdfSplitUtils.deduplicateResources(doc);

                assertSame(entry(gs1), entry(gs2));
            }
        }

        @Test
        @DisplayName("digestCOSTree: COSName → 'N' marker + name bytes (interned singleton, code path exercised)")
        void digestCOSTree_cosName_codePathExercised() throws IOException {
            // COSName.getPDFName is interned, so both pages reference the SAME Java object.
            // existing == obj → no replacement, but the hash IS computed (branch hit).
            try (PDDocument doc = new PDDocument()) {
                COSName name = COSName.getPDFName("DeviceRGB");
                addGsEntry(doc, name);
                addGsEntry(doc, name);
                assertDoesNotThrow(() -> PdfSplitUtils.deduplicateResources(doc));
            }
        }

        @Test
        @DisplayName("digestCOSTree: COSString → 'T' marker + raw bytes, identical strings deduplicated")
        void digestCOSTree_cosString_hashedAndDeduplicated() throws IOException {
            COSString str1 = new COSString("catalog-value");
            COSString str2 = new COSString("catalog-value");

            try (PDDocument doc = new PDDocument()) {
                COSDictionary gs1 = addGsEntry(doc, str1);
                COSDictionary gs2 = addGsEntry(doc, str2);

                PdfSplitUtils.deduplicateResources(doc);

                assertSame(entry(gs1), entry(gs2),
                        "Identical COSStrings should be deduplicated");
            }
        }

        @Test
        @DisplayName("digestCOSTree: COSNumber → '#' marker + floatValue bytes, identical numbers deduplicated")
        void digestCOSTree_cosNumber_hashedAndDeduplicated() throws IOException {
            COSFloat f1 = new COSFloat(3.14f);
            COSFloat f2 = new COSFloat(3.14f);

            try (PDDocument doc = new PDDocument()) {
                COSDictionary gs1 = addGsEntry(doc, f1);
                COSDictionary gs2 = addGsEntry(doc, f2);

                PdfSplitUtils.deduplicateResources(doc);

                assertSame(entry(gs1), entry(gs2),
                        "Identical COSNumbers should be deduplicated");
            }
        }

        @Test
        @DisplayName("digestCOSTree: COSBoolean TRUE → byte 1 written (ternary true-branch)")
        void digestCOSTree_cosBooleanTrue_byte1Written() throws IOException {
            // COSBoolean.TRUE is a singleton → existing == obj → no replacement,
            // but digestCOSTree IS called and the `? (byte) 1` branch is taken.
            try (PDDocument doc = new PDDocument()) {
                addGsEntry(doc, COSBoolean.TRUE);
                addGsEntry(doc, COSBoolean.TRUE);
                assertDoesNotThrow(() -> PdfSplitUtils.deduplicateResources(doc));
            }
        }

        @Test
        @DisplayName("digestCOSTree: COSBoolean FALSE → byte 2 written (ternary false-branch)")
        void digestCOSTree_cosBooleanFalse_byte2Written() throws IOException {
            // COSBoolean.FALSE is a singleton → same singleton = no replacement, but branch hit.
            try (PDDocument doc = new PDDocument()) {
                addGsEntry(doc, COSBoolean.FALSE);
                addGsEntry(doc, COSBoolean.FALSE);
                assertDoesNotThrow(() -> PdfSplitUtils.deduplicateResources(doc));
            }
        }

        @Test
        @DisplayName("digestCOSTree: else branch — COSNull (not any known subtype) → obj.toString() bytes")
        void digestCOSTree_elseBranch_cosNullViaArray() throws IOException {
            // COSNull is not COSName/COSString/COSNumber/COSBoolean/COSDictionary/COSArray.
            // Reached via arr.get(i) which returns raw COSBase without resolving.
            COSArray arr1 = new COSArray();
            arr1.add(COSNull.NULL);
            COSArray arr2 = new COSArray();
            arr2.add(COSNull.NULL);

            try (PDDocument doc = new PDDocument()) {
                COSDictionary gs1 = addGsEntry(doc, arr1);
                COSDictionary gs2 = addGsEntry(doc, arr2);

                assertDoesNotThrow(() -> PdfSplitUtils.deduplicateResources(doc));
                // Both arrays hash identically (COSNull.toString() is the same) → dedup
                assertSame(entry(gs1), entry(gs2));
            }
        }

        // =================================================================
        //  digestDictEntries — /Length key skipped
        // =================================================================

        @Test
        @DisplayName("digestDictEntries: /Length key is skipped — streams with same raw content hash equally")
        void digestDictEntries_lengthKeySkipped() throws IOException {
            // COSStream naturally has /Length in its metadata dictionary.
            // digestDictEntries skips /Length → two streams with identical payload
            // but potentially different /Length values still get the same hash.
            byte[] payload = "same raw content".getBytes(StandardCharsets.UTF_8);

            try (PDDocument doc = new PDDocument()) {
                COSStream s1 = new COSStream();
                try (OutputStream os = s1.createOutputStream()) { os.write(payload); }

                COSStream s2 = new COSStream();
                try (OutputStream os = s2.createOutputStream()) { os.write(payload); }

                // An extra non-Length entry to force digestDictEntries to iterate multiple keys
                s1.setItem(COSName.getPDFName("Tag"), COSName.getPDFName("V"));
                s2.setItem(COSName.getPDFName("Tag"), COSName.getPDFName("V"));

                COSDictionary x1 = addXobjEntry(doc, s1);
                COSDictionary x2 = addXobjEntry(doc, s2);

                PdfSplitUtils.deduplicateResources(doc);

                assertSame(xentry(x1), xentry(x2),
                        "Streams with identical raw content should be deduplicated; /Length skip must not break equality");
            }
        }

        // =================================================================
        //  Different content → different hash → NOT deduplicated
        // =================================================================

        @Test
        @DisplayName("different COSDictionary contents → different hashes → not deduplicated")
        void differentContent_notDeduplicated() throws IOException {
            COSDictionary d1 = new COSDictionary();
            d1.setFloat(COSName.CA, 0.5f);
            COSDictionary d2 = new COSDictionary();
            d2.setFloat(COSName.CA, 0.9f); // different

            try (PDDocument doc = new PDDocument()) {
                COSDictionary gs1 = addGsEntry(doc, d1);
                COSDictionary gs2 = addGsEntry(doc, d2);

                PdfSplitUtils.deduplicateResources(doc);

                assertNotSame(entry(gs1), entry(gs2),
                        "Resources with different content must remain distinct");
            }
        }
    }

    // =====================================================================
    //  Helpers (private, shared by all nested classes)
    // =====================================================================

    /** Writes raw ASCII content to a page's content stream. */
    private void writeRawContent(PDDocument doc, PDPage page, String content) throws IOException {
        PDStream stream = new PDStream(doc);
        try (OutputStream os = stream.createOutputStream()) {
            os.write(content.getBytes(StandardCharsets.US_ASCII));
        }
        page.getCOSObject().setItem(COSName.CONTENTS, stream.getCOSObject());
    }

    /**
     * Creates a 3-page PDF with document-level catalog objects:
     * OutputIntents (with a large-ish ICC profile stream), StructTreeRoot,
     * AcroForm, Names, Outlines, Metadata, MarkInfo, Threads, and OpenAction.
     * Each page also has text content so we can verify it survives the strip.
     */
    private File createPdfWithDocumentLevelObjects() throws IOException {
        File file = File.createTempFile("doclevel_", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            // Add 3 pages with text
            for (int i = 0; i < 3; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(50, 700);
                    cs.showText("Page " + (i + 1) + " content");
                    cs.endText();
                }
            }

            COSDictionary catalogDict = doc.getDocumentCatalog().getCOSObject();

            // OutputIntents with a fake ICC profile stream (pad to ~50 KB to simulate real overhead)
            COSDictionary oiDict = new COSDictionary();
            oiDict.setItem(COSName.TYPE, COSName.getPDFName("OutputIntent"));
            oiDict.setItem(COSName.S, COSName.getPDFName("GTS_PDFA1"));
            COSStream iccStream = new COSStream();
            try (OutputStream os = iccStream.createOutputStream()) {
                os.write(new byte[50_000]); // simulate ICC profile data
            }
            oiDict.setItem(COSName.getPDFName("DestOutputProfile"), iccStream);
            COSArray oiArray = new COSArray();
            oiArray.add(oiDict);
            catalogDict.setItem(COSName.OUTPUT_INTENTS, oiArray);

            // StructTreeRoot
            COSDictionary structTree = new COSDictionary();
            structTree.setItem(COSName.TYPE, COSName.getPDFName("StructTreeRoot"));
            catalogDict.setItem(COSName.STRUCT_TREE_ROOT, structTree);

            // AcroForm
            PDAcroForm acroForm = new PDAcroForm(doc);
            catalogDict.setItem(COSName.ACRO_FORM, acroForm.getCOSObject());

            // Names dictionary
            COSDictionary names = new COSDictionary();
            names.setItem(COSName.getPDFName("EmbeddedFiles"), new COSDictionary());
            catalogDict.setItem(COSName.NAMES, names);

            // Outlines (bookmarks)
            COSDictionary outlines = new COSDictionary();
            outlines.setItem(COSName.TYPE, COSName.getPDFName("Outlines"));
            catalogDict.setItem(COSName.OUTLINES, outlines);

            // Metadata (XMP stream)
            COSStream metaStream = new COSStream();
            try (OutputStream os = metaStream.createOutputStream()) {
                os.write("<xmp>fake metadata</xmp>".getBytes(StandardCharsets.UTF_8));
            }
            catalogDict.setItem(COSName.METADATA, metaStream);

            // MarkInfo
            COSDictionary markInfo = new COSDictionary();
            markInfo.setBoolean(COSName.getPDFName("Marked"), true);
            catalogDict.setItem(COSName.getPDFName("MarkInfo"), markInfo);

            // Threads
            COSArray threads = new COSArray();
            threads.add(new COSDictionary());
            catalogDict.setItem(COSName.THREADS, threads);

            // OpenAction
            COSDictionary openAction = new COSDictionary();
            openAction.setItem(COSName.TYPE, COSName.getPDFName("Action"));
            openAction.setItem(COSName.S, COSName.getPDFName("GoTo"));
            catalogDict.setItem(COSName.OPEN_ACTION, openAction);

            doc.save(file);
        }
        return file;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // collectUsedResourceNames — 100% branch coverage
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("collectUsedResourceNames")
    class CollectUsedResourceNames {

        private PDDocument doc;
        private Method method;

        @BeforeEach
        void setUp() throws NoSuchMethodException {
            doc = new PDDocument();
            method = PdfSplitUtils.class.getDeclaredMethod("collectUsedResourceNames", PDPage.class);
            method.setAccessible(true);
        }

        @AfterEach
        void tearDown() throws IOException {
            doc.close();
        }

        @SuppressWarnings("unchecked")
        private Set<COSName> invoke(PDPage page) throws Exception {
            try {
                return (Set<COSName>) method.invoke(null, page);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) throw (Exception) cause;
                throw new RuntimeException(cause);
            }
        }

        private PDPage createPageWithContent(String content) throws IOException {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDStream stream = new PDStream(doc);
            try (OutputStream out = stream.createOutputStream()) {
                out.write(content.getBytes(StandardCharsets.ISO_8859_1));
            }
            page.setContents(stream);
            return page;
        }

        // Branch 1: contentStream == null → early return with empty set
        @Test
        @DisplayName("returns empty set when page has no content stream")
        void nullContentStream_returnsEmptySet() throws Exception {
            PDPage page = new PDPage();
            doc.addPage(page);
            // no content stream set → getContents() returns null

            Set<COSName> result = invoke(page);

            assertTrue(result.isEmpty());
        }

        // Branch 2: Operator in RESOURCE_OPERATORS + COSName operand → added
        @Test
        @DisplayName("COSName operand before resource operator is added to result")
        void cosNameBeforeResourceOperator_isAdded() throws Exception {
            // /Im0 Do — Do is a resource operator; /Im0 is the XObject name
            PDPage page = createPageWithContent("/Im0 Do\n");

            Set<COSName> result = invoke(page);

            assertTrue(result.contains(COSName.getPDFName("Im0")));
        }

        @Test
        @DisplayName("COSName operands from multiple resource operators are all added")
        void multipleResourceOperators_allCOSNamesAdded() throws Exception {
            // /F1 Tf sets font; /GS1 gs sets graphics state
            PDPage page = createPageWithContent("/F1 Tf\n/GS1 gs\n");

            Set<COSName> result = invoke(page);

            assertTrue(result.contains(COSName.getPDFName("F1")));
            assertTrue(result.contains(COSName.getPDFName("GS1")));
        }

        // Branch 3: Operator in RESOURCE_OPERATORS + non-COSName operand → NOT added
        @Test
        @DisplayName("non-COSName operand before resource operator is not added")
        void nonCOSNameOperandBeforeResourceOperator_notAdded() throws Exception {
            // integer 1 precedes Do — not a COSName, must not enter usedNames
            PDPage page = createPageWithContent("1 Do\n");

            Set<COSName> result = invoke(page);

            assertTrue(result.isEmpty());
        }

        // Branch 4: Operator NOT in RESOURCE_OPERATORS → clears window, nothing added
        @Test
        @DisplayName("non-resource operator clears window so prior names are not added")
        void nonResourceOperator_clearsWindow_priorNameNotAdded() throws Exception {
            // /Im0 enters window → 'q' (save graphics state, not a resource op) clears it
            // → Do fires with empty window → nothing added for Im0
            PDPage page = createPageWithContent("/Im0 q Do\n");

            Set<COSName> result = invoke(page);

            assertFalse(result.contains(COSName.getPDFName("Im0")));
            assertTrue(result.isEmpty());
        }

        // Branch 5a: operandWindow.size() < 4 → no removal, token just added
        @Test
        @DisplayName("all tokens retained when window is below capacity")
        void windowBelowCapacity_allTokensRetained() throws Exception {
            // 3 tokens (< 4) before Do → size never reaches 4, remove(0) never called
            PDPage page = createPageWithContent("/A /B /C Do\n");

            Set<COSName> result = invoke(page);

            assertTrue(result.contains(COSName.getPDFName("A")));
            assertTrue(result.contains(COSName.getPDFName("B")));
            assertTrue(result.contains(COSName.getPDFName("C")));
        }

        // Branch 5b: operandWindow.size() >= 4 → remove(0) fires before add
        @Test
        @DisplayName("oldest token evicted when window reaches capacity of 4")
        void windowAtCapacity_oldestTokenEvicted() throws Exception {
            // 5 COSName tokens before Do:
            //   /A /B /C /D fill window to 4, then /E triggers remove(/A) → window=[/B,/C,/D,/E]
            //   Do fires: /B /C /D /E added; /A must NOT appear in result
            PDPage page = createPageWithContent("/A /B /C /D /E Do\n");

            Set<COSName> result = invoke(page);

            assertFalse(result.contains(COSName.getPDFName("A")), "/A should have been evicted from window");
            assertTrue(result.contains(COSName.getPDFName("B")));
            assertTrue(result.contains(COSName.getPDFName("C")));
            assertTrue(result.contains(COSName.getPDFName("D")));
            assertTrue(result.contains(COSName.getPDFName("E")));
        }

        // Branch 6: IOException during parsing → catch block executed, addAllResourceNames called
        @Test
        @DisplayName("IOException during parsing triggers fallback to addAllResourceNames")
        void ioExceptionDuringParsing_catchBlockExecuted() throws Exception {
            PDResources resources = new PDResources();

            // Anonymous subclass: first getContents() returns non-null (passes null check),
            // second call (inside PDFStreamParser constructor) throws IOException → catch fires
            PDPage page = new PDPage() {
                private int callCount = 0;

                @Override
                public InputStream getContents() throws IOException {
                    if (++callCount == 1) {
                        return new ByteArrayInputStream(new byte[]{0});
                    }
                    throw new IOException("simulated content stream failure");
                }

                @Override
                public PDResources getResources() {
                    return resources;
                }
            };
            doc.addPage(page);

            // Must NOT propagate the IOException — it is caught and handled internally
            Set<COSName> result = invoke(page);

            assertNotNull(result);
        }
    }
}
