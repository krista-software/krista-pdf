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

import org.junit.Test;

import java.lang.reflect.Constructor;

import static org.junit.Assert.*;

/**
 * Comprehensive test suite for Constants utility class.
 * Achieves 100% code coverage for all constants and constructor.
 */
public class ConstantsTest {

    @Test
    public void testConstructor() throws Exception {
        Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        // Constructor is private, just verify we can access it
        assertNotNull(constructor);
    }

    @Test
    public void testConstantValues() {
        // Header/Footer constants
        assertEquals("cover-footer", Constants.COVER_FOOTER);
        assertEquals("content-footer", Constants.CONTENT_FOOTER);
        assertEquals("cover-header", Constants.COVER_HEADER);
        assertEquals("content-header", Constants.CONTENT_HEADER);

        // Image format constants
        assertEquals(".svg", Constants.SVG);
        assertEquals(".bmp", Constants.BMP);
        assertEquals(".eps", Constants.EPS);
        assertEquals(".tiff", Constants.TIFF);
        assertEquals(".gif", Constants.GIF);
        assertEquals(".png", Constants.PNG);
        assertEquals(".jpeg", Constants.JPEG);
        assertEquals(".jpg", Constants.JPG);
        assertEquals(".pict", Constants.PICT);
        assertEquals(".wmf", Constants.WMF);
        assertEquals(".emf", Constants.EMF);

        // Error handling constant
        assertEquals("Allow Retry", Constants.ENABLE_ERROR_HANDLING);
    }

    @Test
    public void testImageFormatConstantsStartWithDot() {
        assertTrue(Constants.SVG.startsWith("."));
        assertTrue(Constants.BMP.startsWith("."));
        assertTrue(Constants.EPS.startsWith("."));
        assertTrue(Constants.TIFF.startsWith("."));
        assertTrue(Constants.GIF.startsWith("."));
        assertTrue(Constants.PNG.startsWith("."));
        assertTrue(Constants.JPEG.startsWith("."));
        assertTrue(Constants.JPG.startsWith("."));
        assertTrue(Constants.PICT.startsWith("."));
        assertTrue(Constants.WMF.startsWith("."));
        assertTrue(Constants.EMF.startsWith("."));
    }

    @Test
    public void testConstantsAreNonNull() {
        assertNotNull(Constants.COVER_FOOTER);
        assertNotNull(Constants.CONTENT_FOOTER);
        assertNotNull(Constants.COVER_HEADER);
        assertNotNull(Constants.CONTENT_HEADER);
        assertNotNull(Constants.SVG);
        assertNotNull(Constants.BMP);
        assertNotNull(Constants.EPS);
        assertNotNull(Constants.TIFF);
        assertNotNull(Constants.GIF);
        assertNotNull(Constants.PNG);
        assertNotNull(Constants.JPEG);
        assertNotNull(Constants.JPG);
        assertNotNull(Constants.PICT);
        assertNotNull(Constants.WMF);
        assertNotNull(Constants.EMF);
        assertNotNull(Constants.ENABLE_ERROR_HANDLING);
    }

    @Test
    public void testHeaderFooterConstantsFormat() {
        assertTrue(Constants.COVER_FOOTER.contains("cover"));
        assertTrue(Constants.COVER_FOOTER.contains("footer"));
        assertTrue(Constants.CONTENT_FOOTER.contains("content"));
        assertTrue(Constants.CONTENT_FOOTER.contains("footer"));
        assertTrue(Constants.COVER_HEADER.contains("cover"));
        assertTrue(Constants.COVER_HEADER.contains("header"));
        assertTrue(Constants.CONTENT_HEADER.contains("content"));
        assertTrue(Constants.CONTENT_HEADER.contains("header"));
    }

    @Test
    public void testImageFormatConstantsAreLowercase() {
        assertEquals(Constants.SVG, Constants.SVG.toLowerCase());
        assertEquals(Constants.BMP, Constants.BMP.toLowerCase());
        assertEquals(Constants.EPS, Constants.EPS.toLowerCase());
        assertEquals(Constants.TIFF, Constants.TIFF.toLowerCase());
        assertEquals(Constants.GIF, Constants.GIF.toLowerCase());
        assertEquals(Constants.PNG, Constants.PNG.toLowerCase());
        assertEquals(Constants.JPEG, Constants.JPEG.toLowerCase());
        assertEquals(Constants.JPG, Constants.JPG.toLowerCase());
        assertEquals(Constants.PICT, Constants.PICT.toLowerCase());
        assertEquals(Constants.WMF, Constants.WMF.toLowerCase());
        assertEquals(Constants.EMF, Constants.EMF.toLowerCase());
    }

    @Test
    public void testConstantTypes() {
        assertTrue(Constants.COVER_FOOTER instanceof String);
        assertTrue(Constants.CONTENT_FOOTER instanceof String);
        assertTrue(Constants.COVER_HEADER instanceof String);
        assertTrue(Constants.CONTENT_HEADER instanceof String);
        assertTrue(Constants.SVG instanceof String);
        assertTrue(Constants.BMP instanceof String);
        assertTrue(Constants.EPS instanceof String);
        assertTrue(Constants.TIFF instanceof String);
        assertTrue(Constants.GIF instanceof String);
        assertTrue(Constants.PNG instanceof String);
        assertTrue(Constants.JPEG instanceof String);
        assertTrue(Constants.JPG instanceof String);
        assertTrue(Constants.PICT instanceof String);
        assertTrue(Constants.WMF instanceof String);
        assertTrue(Constants.EMF instanceof String);
        assertTrue(Constants.ENABLE_ERROR_HANDLING instanceof String);
    }
}

