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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PDFHelper.sanitizeFileName")
class PDFHelperSanitizeFileNameTest {

    @Test
    @DisplayName("preserves case and inner spacing when no extension is present")
    void preservesCaseAndSpacing() {
        assertEquals("Invoice March", PDFHelper.sanitizeFileName("Invoice March"));
    }

    @Test
    @DisplayName("strips a .pdf extension typed in the name")
    void stripsPdfExtension() {
        assertEquals("Invoice", PDFHelper.sanitizeFileName("Invoice.pdf"));
    }

    @Test
    @DisplayName("strips a non-pdf extension typed in the name")
    void stripsOtherExtension() {
        assertEquals("Invoice", PDFHelper.sanitizeFileName("Invoice.docx"));
    }

    @Test
    @DisplayName("removes special characters but keeps spaces and case")
    void removesSpecialCharacters() {
        assertEquals("Q3 Report v2", PDFHelper.sanitizeFileName("Q3 Report: v2"));
    }

    @Test
    @DisplayName("trims leading and trailing whitespace")
    void trimsEnds() {
        assertEquals("My File", PDFHelper.sanitizeFileName("  My File  "));
    }

    @Test
    @DisplayName("keeps accented letters")
    void keepsAccentedLetters() {
        assertEquals("Café", PDFHelper.sanitizeFileName("Café"));
    }

    @Test
    @DisplayName("returns empty when the name has only special characters")
    void emptyWhenOnlySpecialChars() {
        assertEquals("", PDFHelper.sanitizeFileName("***"));
    }

    @Test
    @DisplayName("returns empty for null input")
    void emptyForNull() {
        assertEquals("", PDFHelper.sanitizeFileName(null));
    }
}
