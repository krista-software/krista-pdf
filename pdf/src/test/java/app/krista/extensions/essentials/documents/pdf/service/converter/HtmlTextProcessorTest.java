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

package app.krista.extensions.essentials.documents.pdf.service.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HtmlTextProcessor}.
 * Covers the public pipeline {@link HtmlTextProcessor#buildHtmlFromText(String)}
 * (which drives private classifyLine/isHeadingCandidate) across every
 * {@link LineType} branch, plus the public static helpers
 * {@link HtmlTextProcessor#removeListMarker(String)} and
 * {@link HtmlTextProcessor#escapeHtml(String)}.
 */
@DisplayName("HtmlTextProcessor Tests")
class HtmlTextProcessorTest {

    private HtmlTextProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new HtmlTextProcessor();
    }

    // ---------------------------------------------------------------
    // Heading classification branches (isHeadingCandidate)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("A single ALL-CAPS short line is classified as a heading")
    void testBuildHtml_AllCapsHeading() {
        String html = processor.buildHtmlFromText("INTRODUCTION");

        assertEquals("    <h2>INTRODUCTION</h2>\n", html);
    }

    @Test
    @DisplayName("A Title Case line (mostly-cap-start, not all caps) is classified as a heading")
    void testBuildHtml_TitleCaseHeading() {
        String html = processor.buildHtmlFromText("Chapter One Summary");

        assertEquals("    <h2>Chapter One Summary</h2>\n", html);
    }

    @Test
    @DisplayName("allCaps=true but mostlyCapStart=false still qualifies as a heading (OR branch)")
    void testBuildHtml_AllCapsButNotMostlyCapStart_StillHeading() {
        // Digits don't change case, so this is "all caps" by .toUpperCase() equality,
        // yet only 1 of 4 space-separated tokens starts with an uppercase letter.
        String html = processor.buildHtmlFromText("123 456 789 ABC");

        assertEquals("    <h2>123 456 789 ABC</h2>\n", html);
    }

    @Test
    @DisplayName("Neither all-caps nor mostly-cap-start -> falls through to paragraph")
    void testBuildHtml_LowerCaseLine_IsParagraph() {
        String html = processor.buildHtmlFromText("the quick brown fox jumps");

        assertEquals("    <p>the quick brown fox jumps</p>\n", html);
    }

    @Test
    @DisplayName("A trailing period suppresses heading classification even for an all-caps line")
    void testBuildHtml_EndsWithPeriod_SuppressesHeading() {
        String html = processor.buildHtmlFromText("ALL CAPS LINE.");

        assertEquals("    <p>ALL CAPS LINE.</p>\n", html);
    }

    @Test
    @DisplayName("A trailing colon suppresses heading classification even for a title-case line")
    void testBuildHtml_EndsWithColon_SuppressesHeading() {
        String html = processor.buildHtmlFromText("Important Notes:");

        assertEquals("    <p>Important Notes:</p>\n", html);
    }

    @Test
    @DisplayName("A line longer than 80 characters is never classified as a heading")
    void testBuildHtml_LineTooLong_IsParagraph() {
        String longLine = "A".repeat(90);

        String html = processor.buildHtmlFromText(longLine);

        assertEquals("    <p>" + longLine + "</p>\n", html);
    }

    // ---------------------------------------------------------------
    // List classification branches
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Two numeric ordered items ('1.' style) produce a well-formed <ol>")
    void testBuildHtml_OrderedList_DotMarker() {
        String html = processor.buildHtmlFromText("1. First item\n2. Second item");

        assertEquals("    <ol>\n      <li>First item</li>\n      <li>Second item</li>\n    </ol>\n", html);
    }

    @Test
    @DisplayName("A numeric ordered item using the ')' marker is recognized")
    void testBuildHtml_OrderedList_ParenMarker() {
        String html = processor.buildHtmlFromText("2) Second item");

        assertEquals("    <ol>\n      <li>Second item</li>\n    </ol>\n", html);
    }

    @Test
    @DisplayName("Two alphabetical ordered items ('a.' style) produce a well-formed <ol type=\"a\">")
    void testBuildHtml_AlphaList_DotMarker() {
        String html = processor.buildHtmlFromText("a. Apple\nb. Banana");

        assertEquals(
                "\n        <ol type=\"a\">\n          <li>Apple</li>\n          <li>Banana</li>\n      </ol>\n",
                html);
    }

    @Test
    @DisplayName("An uppercase alphabetical marker with ')' delimiter is recognized")
    void testBuildHtml_AlphaList_UppercaseParenMarker() {
        String html = processor.buildHtmlFromText("B) Banana");

        assertEquals("\n        <ol type=\"a\">\n          <li>Banana</li>\n      </ol>\n", html);
    }

    @Test
    @DisplayName("Bullet items using '-', '*' and the bullet-dot marker all produce a well-formed <ul>")
    void testBuildHtml_BulletList_AllMarkerVariants() {
        String html = processor.buildHtmlFromText("- First\n* Second\n• Third");

        assertEquals("    <ul>\n      <li>First</li>\n      <li>Second</li>\n      <li>Third</li>\n    </ul>\n",
                html);
    }

    // ---------------------------------------------------------------
    // Line splitting / empty line handling
    // ---------------------------------------------------------------

    @Test
    @DisplayName("A trailing blank line after a list item closes the <li> early but leaves the <ol> open until closeAll")
    void testBuildHtml_TrailingBlankLineAfterListItem() {
        String html = processor.buildHtmlFromText("1. Item\n");

        assertEquals("    <ol>\n      <li>Item</li>\n    </ol>\n", html);
    }

    @Test
    @DisplayName("Mixed \\r\\n, lone \\r and \\n line separators are all split correctly")
    void testBuildHtml_MixedLineSeparators() {
        String html = processor.buildHtmlFromText("1. One\r\n2. Two\r3. Three\n");

        assertEquals(
                "    <ol>\n      <li>One</li>\n      <li>Two</li>\n      <li>Three</li>\n    </ol>\n",
                html);
    }

    @Test
    @DisplayName("Text consisting only of blank/whitespace lines produces no HTML output")
    void testBuildHtml_OnlyBlankLines() {
        String html = processor.buildHtmlFromText("   \n\t\n  ");

        assertEquals("", html);
    }

    @Test
    @DisplayName("An empty string input produces no HTML output")
    void testBuildHtml_EmptyString() {
        String html = processor.buildHtmlFromText("");

        assertEquals("", html);
    }

    @Test
    @DisplayName("A null input throws NullPointerException")
    void testBuildHtml_NullInput_Throws() {
        assertThrows(NullPointerException.class, () -> processor.buildHtmlFromText(null));
    }

    @Test
    @DisplayName("A simple sentence paragraph is wrapped in <p> and closed by closeAll")
    void testBuildHtml_SimpleParagraph() {
        String html = processor.buildHtmlFromText("This is a simple sentence.");

        assertEquals("    <p>This is a simple sentence.</p>\n", html);
    }

    // ---------------------------------------------------------------
    // removeListMarker
    // ---------------------------------------------------------------

    @Test
    @DisplayName("removeListMarker strips numeric markers using '.' or ')' delimiters")
    void testRemoveListMarker_NumericMarkers() {
        assertEquals("Item", HtmlTextProcessor.removeListMarker("1. Item"));
        assertEquals("Item", HtmlTextProcessor.removeListMarker("2) Item"));
    }

    @Test
    @DisplayName("removeListMarker strips alphabetical markers regardless of case or delimiter")
    void testRemoveListMarker_AlphaMarkers() {
        assertEquals("Item", HtmlTextProcessor.removeListMarker("a. Item"));
        assertEquals("Item", HtmlTextProcessor.removeListMarker("B) Item"));
    }

    @Test
    @DisplayName("removeListMarker strips bullet markers: '-', '*' and the bullet-dot")
    void testRemoveListMarker_BulletMarkers() {
        assertEquals("Item", HtmlTextProcessor.removeListMarker("- Item"));
        assertEquals("Item", HtmlTextProcessor.removeListMarker("* Item"));
        assertEquals("Item", HtmlTextProcessor.removeListMarker("• Item"));
    }

    @Test
    @DisplayName("removeListMarker leaves a line with no marker unchanged")
    void testRemoveListMarker_NoMarker() {
        assertEquals("No marker here", HtmlTextProcessor.removeListMarker("No marker here"));
    }

    // ---------------------------------------------------------------
    // escapeHtml
    // ---------------------------------------------------------------

    @Test
    @DisplayName("escapeHtml escapes &, <, >, \" and ' characters")
    void testEscapeHtml_AllSpecialCharacters() {
        String input = "<a href=\"x\">O'Brien & Co</a>";
        String expected = "&lt;a href=&quot;x&quot;&gt;O&#39;Brien &amp; Co&lt;/a&gt;";

        assertEquals(expected, HtmlTextProcessor.escapeHtml(input));
    }

    @Test
    @DisplayName("escapeHtml returns an empty string for empty input")
    void testEscapeHtml_EmptyString() {
        assertEquals("", HtmlTextProcessor.escapeHtml(""));
    }

    @Test
    @DisplayName("escapeHtml leaves plain text without special characters unchanged")
    void testEscapeHtml_PlainText() {
        assertEquals("plain text", HtmlTextProcessor.escapeHtml("plain text"));
    }

    @Test
    @DisplayName("Default constructor produces a usable processor instance")
    void testDefaultConstructor() {
        assertNotNull(new HtmlTextProcessor());
    }
}
