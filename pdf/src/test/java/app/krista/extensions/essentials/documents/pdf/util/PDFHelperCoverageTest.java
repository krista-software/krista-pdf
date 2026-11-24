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

import app.krista.model.base.EntityValue;
import app.krista.model.base.FreeForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.docx4j.openpackaging.contenttype.ContentTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * New JUnit 5 coverage tests for {@link PDFHelper}, targeting methods and branches
 * that are not exercised by the (currently non-executing, JUnit 4) PDFHelperTest.
 */
class PDFHelperCoverageTest {

    // ---------------------------------------------------------------
    // Private constructor
    // ---------------------------------------------------------------

    @Test
    void privateConstructor_throwsIllegalStateException() throws Exception {
        Constructor<PDFHelper> constructor = PDFHelper.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertEquals("Utility class", ex.getCause().getMessage());
    }

    // ---------------------------------------------------------------
    // getValidatedFreeFormData
    // ---------------------------------------------------------------

    @Test
    void getValidatedFreeFormData_textType_passesThroughUnchanged() {
        FreeForm input = new FreeForm();
        input.put("name", "Text", "John Doe");

        FreeForm result = PDFHelper.getValidatedFreeFormData(input);

        assertEquals("John Doe", result.get("name"));
        assertEquals("Text", result.getTypeDescription("name"));
    }

    @Test
    void getValidatedFreeFormData_numberType_withDateKeyAndScientificNotation_convertsToDateString() {
        FreeForm input = new FreeForm();
        input.put("$$date", "Number", 1.7023488E12);

        FreeForm result = PDFHelper.getValidatedFreeFormData(input);

        assertEquals("Text", result.getTypeDescription("$$date"));
        Object value = result.get("$$date");
        assertInstanceOf(String.class, value);
        assertTrue(((String) value).matches("\\d{2}-\\d{2}-\\d{4} \\d{2}\\.\\d{2}\\.\\d{2}"));
    }

    @Test
    void getValidatedFreeFormData_numberType_withDateKeyButNoScientificNotation_keepsOriginal() {
        FreeForm input = new FreeForm();
        input.put("$$date", "Number", 1000.0);

        FreeForm result = PDFHelper.getValidatedFreeFormData(input);

        assertEquals("Number", result.getTypeDescription("$$date"));
        assertEquals(1000.0, result.get("$$date"));
    }

    @Test
    void getValidatedFreeFormData_switchType_true_mapsToYes() {
        FreeForm input = new FreeForm();
        input.put("active", "Switch", true);

        FreeForm result = PDFHelper.getValidatedFreeFormData(input);

        assertEquals("Yes", result.get("active"));
        assertEquals("Text", result.getTypeDescription("active"));
    }

    @Test
    void getValidatedFreeFormData_switchType_false_mapsToNo() {
        FreeForm input = new FreeForm();
        input.put("active", "Switch", false);

        FreeForm result = PDFHelper.getValidatedFreeFormData(input);

        assertEquals("No", result.get("active"));
    }

    // ---------------------------------------------------------------
    // getEntityDataAsList
    // ---------------------------------------------------------------

    @Test
    void getEntityDataAsList_emptyList_returnsEmptyList() {
        List<EntityValue> entityValues = new ArrayList<>();
        List<Map<String, Object>> result = PDFHelper.getEntityDataAsList(entityValues);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getEntityDataAsList_withEntries_returnsFieldMaps() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", 1);
        EntityValue entity = new EntityValue("TestEntity", fields);

        List<Map<String, Object>> result = PDFHelper.getEntityDataAsList(List.of(entity));

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("id"));
    }

    // ---------------------------------------------------------------
    // getDateForDoubleValue
    // ---------------------------------------------------------------

    @Test
    void getDateForDoubleValue_validDateKey_withoutScientificNotation_returnsNull() {
        assertNull(PDFHelper.getDateForDoubleValue("$$date", 1000.0));
    }

    @Test
    void getDateForDoubleValue_invalidKey_withScientificNotation_returnsNull() {
        assertNull(PDFHelper.getDateForDoubleValue("$$amount", 1.7023488E12));
    }

    @Test
    void getDateForDoubleValue_caseInsensitiveKeyMatch_returnsFormattedDate() {
        String result = PDFHelper.getDateForDoubleValue("$$DATE", 1.7023488E12);
        assertNotNull(result);
        assertTrue(result.matches("\\d{2}-\\d{2}-\\d{4} \\d{2}\\.\\d{2}\\.\\d{2}"));
    }

    // ---------------------------------------------------------------
    // cleanUp
    // ---------------------------------------------------------------

    @Test
    void cleanUp_existingFile_deletesIt(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("toDelete.tmp");
        Files.writeString(file, "data");
        assertTrue(Files.exists(file));

        PDFHelper.cleanUp(file);

        assertFalse(Files.exists(file));
    }

    @Test
    void cleanUp_nonExistentFile_throwsIOException(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("doesNotExist.tmp");
        assertThrows(NoSuchFileException.class, () -> PDFHelper.cleanUp(missing));
    }

    // ---------------------------------------------------------------
    // getBytesArrayForLogo
    // ---------------------------------------------------------------

    @Test
    void getBytesArrayForLogo_normalRead_returnsExactBytes(@TempDir Path tempDir) throws IOException {
        File logo = tempDir.resolve("logo.png").toFile();
        byte[] data = "hello world logo bytes".getBytes();
        Files.write(logo.toPath(), data);

        try (InputStream is = new FileInputStream(logo)) {
            byte[] result = PDFHelper.getBytesArrayForLogo(logo, is);
            assertArrayEquals(data, result);
        }
    }

    @Test
    void getBytesArrayForLogo_emptyFile_returnsEmptyArray(@TempDir Path tempDir) throws IOException {
        File logo = tempDir.resolve("empty.png").toFile();
        Files.write(logo.toPath(), new byte[0]);

        try (InputStream is = new FileInputStream(logo)) {
            byte[] result = PDFHelper.getBytesArrayForLogo(logo, is);
            assertEquals(0, result.length);
        }
    }

    @Test
    void getBytesArrayForLogo_lengthExceedsIntegerMax_throwsIllegalArgumentException() {
        File mockFile = mock(File.class);
        when(mockFile.length()).thenReturn((long) Integer.MAX_VALUE + 1);
        InputStream dummy = new ByteArrayInputStream(new byte[0]);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PDFHelper.getBytesArrayForLogo(mockFile, dummy));
        assertTrue(ex.getMessage().contains("too large"));
    }

    @Test
    void getBytesArrayForLogo_exceeds10MB_throwsIllegalArgumentException() {
        File mockFile = mock(File.class);
        when(mockFile.length()).thenReturn(11L * 1024 * 1024);
        when(mockFile.getName()).thenReturn("huge.png");
        InputStream dummy = new ByteArrayInputStream(new byte[0]);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PDFHelper.getBytesArrayForLogo(mockFile, dummy));
        assertTrue(ex.getMessage().contains("10MB"));
    }

    @Test
    void getBytesArrayForLogo_truncatedStream_throwsIOException(@TempDir Path tempDir) throws IOException {
        File logo = tempDir.resolve("truncated.png").toFile();
        byte[] data = new byte[20];
        Arrays.fill(data, (byte) 7);
        Files.write(logo.toPath(), data);

        InputStream truncated = new InputStream() {
            int remaining = 5;

            @Override
            public int read() {
                return remaining-- > 0 ? 7 : -1;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (remaining <= 0) {
                    return -1;
                }
                int n = Math.min(len, remaining);
                Arrays.fill(b, off, off + n, (byte) 7);
                remaining -= n;
                return n;
            }
        };

        IOException ex = assertThrows(IOException.class,
                () -> PDFHelper.getBytesArrayForLogo(logo, truncated));
        assertTrue(ex.getMessage().contains("Incomplete file read"));
    }

    // ---------------------------------------------------------------
    // generatePdfFilePath
    // ---------------------------------------------------------------

    @Test
    void generatePdfFilePath_normalName() {
        String path = PDFHelper.generatePdfFilePath("Report.docx");
        assertEquals("/tmp" + File.separator + "Report.pdf", path);
    }

    @Test
    void generatePdfFilePath_noExtension() {
        String path = PDFHelper.generatePdfFilePath("README");
        assertEquals("/tmp" + File.separator + "README.pdf", path);
    }

    @Test
    void generatePdfFilePath_specialCharacters_cleaned() {
        String path = PDFHelper.generatePdfFilePath("my report@2024!.xlsx");
        assertEquals("/tmp" + File.separator + "my_report2024.pdf", path);
    }

    // ---------------------------------------------------------------
    // generateDocxFilePath
    // ---------------------------------------------------------------

    @Test
    void generateDocxFilePath_hasExpectedPrefixSuffixAndUuid() {
        String path = PDFHelper.generateDocxFilePath();
        String prefix = "/tmp" + File.separator + "doc_docx_";
        assertTrue(path.startsWith(prefix));
        assertTrue(path.endsWith(".docx"));
        String uuidPart = path.substring(prefix.length(), path.length() - ".docx".length());
        assertDoesNotThrow(() -> UUID.fromString(uuidPart));
    }

    // ---------------------------------------------------------------
    // cleanFileName
    // ---------------------------------------------------------------

    @Test
    void cleanFileName_null_returnsFile() {
        assertEquals("file", PDFHelper.cleanFileName(null));
    }

    @Test
    void cleanFileName_empty_returnsFile() {
        assertEquals("file", PDFHelper.cleanFileName(""));
    }

    @Test
    void cleanFileName_onlySymbols_returnsFile() {
        assertEquals("file", PDFHelper.cleanFileName("@#$%^&*()"));
    }

    @Test
    void cleanFileName_normalizesMultipleWhitespace() {
        assertEquals("a_b_c", PDFHelper.cleanFileName("a   b   c"));
    }

    @Test
    void cleanFileName_preservesDashesAndUnderscores() {
        assertEquals("my-file_name", PDFHelper.cleanFileName("my-file_name"));
    }

    @Test
    void cleanFileName_removesAccentsViaUnicodeNormalization() {
        assertEquals("cafe", PDFHelper.cleanFileName("café"));
    }

    @Test
    void cleanFileName_removesEmojiAndCollapsesWhitespace() {
        assertEquals("party_time", PDFHelper.cleanFileName("party 🎉 time"));
    }

    // ---------------------------------------------------------------
    // getImageFormat
    // ---------------------------------------------------------------

    @Test
    void getImageFormat_allSupportedExtensions() {
        assertEquals(IMAGE_EMF, PDFHelper.getImageFormat("drawing.emf"));
        assertEquals(IMAGE_WMF, PDFHelper.getImageFormat("drawing.wmf"));
        assertEquals(IMAGE_PICT, PDFHelper.getImageFormat("drawing.pict"));
        assertEquals(IMAGE_JPEG, PDFHelper.getImageFormat("photo.jpeg"));
        assertEquals(IMAGE_JPEG, PDFHelper.getImageFormat("photo.jpg"));
        assertEquals(IMAGE_PNG, PDFHelper.getImageFormat("photo.png"));
        assertEquals(IMAGE_GIF, PDFHelper.getImageFormat("anim.gif"));
        assertEquals(IMAGE_TIFF, PDFHelper.getImageFormat("scan.tiff"));
        assertEquals(IMAGE_EPS, PDFHelper.getImageFormat("vector.eps"));
        assertEquals(IMAGE_BMP, PDFHelper.getImageFormat("bitmap.bmp"));
        assertEquals(IMAGE_SVG, PDFHelper.getImageFormat("vector.svg"));
    }

    @Test
    void getImageFormat_unsupportedExtension_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PDFHelper.getImageFormat("file.xyz"));
        assertTrue(ex.getMessage().contains("Unsupported picture"));
    }

    // ---------------------------------------------------------------
    // getSafeFileName
    // ---------------------------------------------------------------

    @Test
    void getSafeFileName_null_returnsFile() {
        assertEquals("file", PDFHelper.getSafeFileName(null));
    }

    @Test
    void getSafeFileName_empty_returnsFile() {
        assertEquals("file", PDFHelper.getSafeFileName(""));
    }

    @Test
    void getSafeFileName_noSymbols_onlyLowercases() {
        assertEquals("myfile.pdf", PDFHelper.getSafeFileName("MyFile.PDF"));
    }

    @Test
    void getSafeFileName_withSymbols_cleansAndLowercases() {
        assertEquals("filenamepng", PDFHelper.getSafeFileName("File@Name!.png"));
    }

    // ---------------------------------------------------------------
    // resizeImage
    // ---------------------------------------------------------------

    @Test
    void resizeImage_downscalesMaintainingAspectRatio() {
        BufferedImage original = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        BufferedImage resized = PDFHelper.resizeImage(original, 100f, 100f);
        assertEquals(100, resized.getWidth());
        assertEquals(50, resized.getHeight());
    }

    @Test
    void resizeImage_upscalesSmallImage() {
        BufferedImage original = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        BufferedImage resized = PDFHelper.resizeImage(original, 100f, 100f);
        assertEquals(100, resized.getWidth());
        assertEquals(100, resized.getHeight());
    }

    @Test
    void resizeImage_clampsToMinimumOnePixel() {
        BufferedImage original = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        BufferedImage resized = PDFHelper.resizeImage(original, 0.4f, 0.4f);
        assertEquals(1, resized.getWidth());
        assertEquals(1, resized.getHeight());
    }
}
