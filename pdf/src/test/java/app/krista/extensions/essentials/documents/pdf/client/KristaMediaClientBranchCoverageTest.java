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

package app.krista.extensions.essentials.documents.pdf.client;

import app.krista.ksdk.files.FileHandle;
import app.krista.ksdk.files.FileRepository;
import app.krista.model.base.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Supplementary branch-coverage tests for {@link KristaMediaClient}, targeting gaps
 * left by the existing {@code KristaMediaClientTest}.
 *
 * <p>{@code KristaMediaClientTest} already covers: unsupported-format-&gt;zip
 * compression, supported formats bypassing compression, a missing-extension file
 * name (no dot at all), empty vs. non-empty download content (the
 * {@code convertInputStreamToFile} while-loop), and multi-dot file names.</p>
 *
 * <p>This class adds the branches that were NOT yet exercised:</p>
 * <ul>
 *   <li>{@code generateUniqueFile}'s own null-name guard (first OR operand, reached
 *       via {@code toJavaFile} when the Krista file's name is literally null)</li>
 *   <li>{@code generateUniqueFile}'s own blank-name guard (second OR operand, a
 *       non-null but whitespace-only name)</li>
 *   <li>{@code getFileExtension}'s independent blank-name guard, reached via
 *       {@code toKristaFile}/{@code isUnsupportedFileFormat} with an empty file name -
 *       separate bytecode location from {@code generateUniqueFile}'s guard even
 *       though the logic looks identical</li>
 *   <li>{@code getFileExtension}'s {@code lastDotIndex < length-1} operand going
 *       false on its own (a trailing dot with no characters after it, e.g.
 *       {@code "report."}), independent from the {@code lastDotIndex > 0} operand</li>
 *   <li>a hidden/dotfile name ({@code lastDotIndex == 0}) hitting the
 *       {@code lastDotIndex > 0} operand's false outcome from a different input shape
 *       than the "no dot at all" case already covered elsewhere</li>
 * </ul>
 */
@DisplayName("KristaMediaClient Branch Coverage Tests")
class KristaMediaClientBranchCoverageTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileHandle fileHandle;

    @Mock
    private File kristaFile;

    private KristaMediaClient kristaMediaClient;
    private final List<java.io.File> filesToCleanup = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        kristaMediaClient = new KristaMediaClient();

        java.lang.reflect.Field field = KristaMediaClient.class.getDeclaredField("fileRepository");
        field.setAccessible(true);
        field.set(kristaMediaClient, fileRepository);
    }

    @AfterEach
    void tearDown() {
        for (java.io.File f : filesToCleanup) {
            if (f != null && f.exists()) {
                f.delete();
            }
        }
        filesToCleanup.clear();
    }

    @Test
    @DisplayName("toJavaFile: null Krista file name triggers generateUniqueFile's null guard (first OR operand)")
    void testToJavaFile_NullFileName_ThrowsIllegalArgumentException() throws IOException {
        when(kristaFile.getFileName()).thenReturn(null);
        when(fileRepository.getFile(kristaFile)).thenReturn(fileHandle);
        when(fileHandle.getContent()).thenReturn(new ByteArrayInputStream("x".getBytes()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> kristaMediaClient.toJavaFile(kristaFile));
        assertEquals("File name cannot be null or empty", ex.getMessage());

        verify(fileRepository).getFile(kristaFile);
    }

    @Test
    @DisplayName("toJavaFile: whitespace-only Krista file name triggers generateUniqueFile's blank guard (second OR operand)")
    void testToJavaFile_BlankFileName_ThrowsIllegalArgumentException() throws IOException {
        when(kristaFile.getFileName()).thenReturn("   ");
        when(fileRepository.getFile(kristaFile)).thenReturn(fileHandle);
        when(fileHandle.getContent()).thenReturn(new ByteArrayInputStream("x".getBytes()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> kristaMediaClient.toJavaFile(kristaFile));
        assertEquals("File name cannot be null or empty", ex.getMessage());
    }

    @Test
    @DisplayName("toKristaFile: empty file name triggers getFileExtension's own blank guard (independent bytecode from generateUniqueFile's)")
    void testToKristaFile_EmptyFileName_ThrowsIllegalArgumentException() {
        java.io.File emptyNameFile = new java.io.File("");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> kristaMediaClient.toKristaFile(emptyNameFile));
        assertEquals("File name cannot be null or empty", ex.getMessage());

        verifyNoInteractions(fileRepository);
    }

    @Test
    @DisplayName("toKristaFile: trailing dot with nothing after it fails the 'lastDotIndex < length-1' operand")
    void testToKristaFile_TrailingDotNoExtension_ThrowsIllegalArgumentException() throws IOException {
        java.io.File testFile = createTempFileNamed("report.");
        filesToCleanup.add(testFile);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> kristaMediaClient.toKristaFile(testFile));
        assertEquals("Unsupported file format: file has no extension", ex.getMessage());

        verifyNoInteractions(fileRepository);
    }

    @Test
    @DisplayName("toJavaFile: trailing-dot Krista file name fails the same 'lastDotIndex < length-1' operand via generateUniqueFile")
    void testToJavaFile_TrailingDotNoExtension_ThrowsIllegalArgumentException() throws IOException {
        when(kristaFile.getFileName()).thenReturn("downloaded.");
        when(fileRepository.getFile(kristaFile)).thenReturn(fileHandle);
        when(fileHandle.getContent()).thenReturn(new ByteArrayInputStream("x".getBytes()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> kristaMediaClient.toJavaFile(kristaFile));
        assertEquals("Unsupported file format: file has no extension", ex.getMessage());
    }

    @Test
    @DisplayName("toKristaFile: a dotfile name (dot at index 0) fails the 'lastDotIndex > 0' operand")
    void testToKristaFile_HiddenDotfileName_ThrowsIllegalArgumentException() throws IOException {
        java.io.File testFile = createTempFileNamed(".gitignore");
        filesToCleanup.add(testFile);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> kristaMediaClient.toKristaFile(testFile));
        assertEquals("Unsupported file format: file has no extension", ex.getMessage());
    }

    @Test
    @DisplayName("toKristaFile: a supported multi-dot format is uploaded directly without zip compression")
    void testToKristaFile_MultiDotSupportedFormat_NoCompression() throws IOException {
        java.io.File testFile = createTempFileNamed("archive.tar.gz");
        filesToCleanup.add(testFile);

        File mockKristaFile = mock(File.class);
        when(fileRepository.createNewFileByName(testFile.getName())).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(mockKristaFile);

        File result = kristaMediaClient.toKristaFile(testFile);

        assertNotNull(result);
        // "gz" is not in the unsupported list, so the original (non-zip) name is used.
        verify(fileRepository).createNewFileByName(testFile.getName());
        verify(fileRepository, never()).createNewFileByName(contains(".zip"));
    }

    @Test
    @DisplayName("toKristaFile: an unsupported JS file is still compressed to zip (contains() true branch for a different format)")
    void testToKristaFile_JsFormat_CompressesToZip() throws IOException {
        java.io.File testFile = createTempFileNamed("script.js");
        filesToCleanup.add(testFile);

        File mockKristaFile = mock(File.class);
        when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(mockKristaFile);

        File result = kristaMediaClient.toKristaFile(testFile);

        assertNotNull(result);
        verify(fileRepository).createNewFileByName(contains(".zip"));
    }

    @Test
    @DisplayName("getFileExtension: null fileName hits the null-check's first OR operand "
            + "(unreachable via the public API - both call sites guarantee a non-null name - "
            + "so invoked directly via reflection on the private method)")
    void testGetFileExtension_NullFileName_ViaReflection_ThrowsIllegalArgumentException() throws Exception {
        // Both real call sites make this branch unreachable through the public API:
        //  - generateUniqueFile() already throws on null/blank before ever calling
        //    getFileExtension(), so it never reaches this code with a null name.
        //  - toKristaFile() only ever calls it with file.getName(), and
        //    java.io.File#getName() is documented to never return null.
        // We invoke the private method directly to confirm its own defensive null
        // check behaves correctly and to close out this branch's coverage.
        java.lang.reflect.Method getFileExtension =
                KristaMediaClient.class.getDeclaredMethod("getFileExtension", String.class);
        getFileExtension.setAccessible(true);

        java.lang.reflect.InvocationTargetException invocationEx = assertThrows(
                java.lang.reflect.InvocationTargetException.class,
                () -> getFileExtension.invoke(kristaMediaClient, (Object) null));

        assertInstanceOf(IllegalArgumentException.class, invocationEx.getCause());
        assertEquals("File name cannot be null or empty", invocationEx.getCause().getMessage());
    }

    // ==================== Helper Methods ====================

    private java.io.File createTempFileNamed(String fileName) throws IOException {
        Path dir = Files.createTempDirectory("krista_media_branch_test_");
        Path filePath = dir.resolve(fileName);
        Files.write(filePath, "test content".getBytes());
        java.io.File f = filePath.toFile();
        f.deleteOnExit();
        dir.toFile().deleteOnExit();
        return f;
    }
}
