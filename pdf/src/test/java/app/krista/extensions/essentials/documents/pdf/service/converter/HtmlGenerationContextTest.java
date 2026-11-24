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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HtmlGenerationContext covering state tracking, HTML
 * accumulation, and the empty-line / close-all element handling logic.
 */
@DisplayName("HtmlGenerationContext Unit Tests")
public class HtmlGenerationContextTest {

    @Test
    @DisplayName("New context starts with empty html and all flags false")
    void testInitialState() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();

        assertEquals("", ctx.getHtml());
        assertFalse(ctx.isInParagraph());
        assertFalse(ctx.isInOl());
        assertFalse(ctx.isInUl());
        assertFalse(ctx.isInOlAlpha());
        assertFalse(ctx.isInListItem());
    }

    @Test
    @DisplayName("append() accumulates content and supports chaining")
    void testAppendAccumulatesAndChains() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();

        HtmlGenerationContext returned = ctx.append("<p>").append("Hello").append("</p>");

        assertSame(ctx, returned, "append should return the same instance for chaining");
        assertEquals("<p>Hello</p>", ctx.getHtml());
    }

    @Test
    @DisplayName("append() with empty string does not change content")
    void testAppendEmptyString() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        ctx.append("abc").append("");
        assertEquals("abc", ctx.getHtml());
    }

    // ==================== handleEmptyLine() ====================

    @Test
    @DisplayName("handleEmptyLine closes list item and paragraph when both open")
    void testHandleEmptyLine_BothOpen() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        ctx.setInListItem(true);
        ctx.setInParagraph(true);

        ctx.handleEmptyLine();

        assertEquals("</li>\n</p>\n", ctx.getHtml());
        assertFalse(ctx.isInListItem());
        assertFalse(ctx.isInParagraph());
    }

    @Test
    @DisplayName("handleEmptyLine closes only list item when paragraph is not open")
    void testHandleEmptyLine_OnlyListItemOpen() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        ctx.setInListItem(true);

        ctx.handleEmptyLine();

        assertEquals("</li>\n", ctx.getHtml());
        assertFalse(ctx.isInListItem());
        assertFalse(ctx.isInParagraph());
    }

    @Test
    @DisplayName("handleEmptyLine closes only paragraph when list item is not open")
    void testHandleEmptyLine_OnlyParagraphOpen() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        ctx.setInParagraph(true);

        ctx.handleEmptyLine();

        assertEquals("</p>\n", ctx.getHtml());
        assertFalse(ctx.isInParagraph());
    }

    @Test
    @DisplayName("handleEmptyLine is a no-op when nothing is open")
    void testHandleEmptyLine_NothingOpen() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();

        ctx.handleEmptyLine();

        assertEquals("", ctx.getHtml());
        assertFalse(ctx.isInListItem());
        assertFalse(ctx.isInParagraph());
    }

    @Test
    @DisplayName("handleEmptyLine preserves previously appended html")
    void testHandleEmptyLine_PreservesExistingContent() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        ctx.append("<li>item");
        ctx.setInListItem(true);

        ctx.handleEmptyLine();

        assertEquals("<li>item</li>\n", ctx.getHtml());
    }

    // ==================== closeAll() ====================

    @Test
    @DisplayName("closeAll is a no-op when no elements are open")
    void testCloseAll_NothingOpen() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();

        ctx.closeAll();

        assertEquals("", ctx.getHtml());
    }

    @Test
    @DisplayName("closeAll closes every open element in the documented order")
    void testCloseAll_AllOpen() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        ctx.setInListItem(true);
        ctx.setInOlAlpha(true);
        ctx.setInParagraph(true);
        ctx.setInOl(true);
        ctx.setInUl(true);

        ctx.closeAll();

        String expected = "</li>\n" + "      </ol>\n" + "</p>\n" + "    </ol>\n" + "    </ul>\n";
        assertEquals(expected, ctx.getHtml());
    }

    @Test
    @DisplayName("closeAll closes only the list item when only it is open")
    void testCloseAll_OnlyListItem() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        ctx.setInListItem(true);

        ctx.closeAll();

        assertEquals("</li>\n", ctx.getHtml());
    }

    @Test
    @DisplayName("closeAll closes only the alpha ordered list when only it is open")
    void testCloseAll_OnlyOlAlpha() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        ctx.setInOlAlpha(true);

        ctx.closeAll();

        assertEquals("      </ol>\n", ctx.getHtml());
    }

    @Test
    @DisplayName("closeAll closes only the paragraph when only it is open")
    void testCloseAll_OnlyParagraph() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        ctx.setInParagraph(true);

        ctx.closeAll();

        assertEquals("</p>\n", ctx.getHtml());
    }

    @Test
    @DisplayName("closeAll closes only the numeric ordered list when only it is open")
    void testCloseAll_OnlyOl() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        ctx.setInOl(true);

        ctx.closeAll();

        assertEquals("    </ol>\n", ctx.getHtml());
    }

    @Test
    @DisplayName("closeAll closes only the unordered list when only it is open")
    void testCloseAll_OnlyUl() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        ctx.setInUl(true);

        ctx.closeAll();

        assertEquals("    </ul>\n", ctx.getHtml());
    }

    @Test
    @DisplayName("closeAll does not reset the boolean flags")
    void testCloseAll_DoesNotResetFlags() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        ctx.setInOl(true);
        ctx.setInUl(true);

        ctx.closeAll();

        // closeAll only appends closing markup - it intentionally does not flip the flags back
        assertTrue(ctx.isInOl());
        assertTrue(ctx.isInUl());
    }

    // ==================== getters/setters ====================

    @Test
    @DisplayName("Paragraph flag getter/setter round trip")
    void testInParagraphGetterSetter() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        assertFalse(ctx.isInParagraph());
        ctx.setInParagraph(true);
        assertTrue(ctx.isInParagraph());
        ctx.setInParagraph(false);
        assertFalse(ctx.isInParagraph());
    }

    @Test
    @DisplayName("Ordered list flag getter/setter round trip")
    void testInOlGetterSetter() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        assertFalse(ctx.isInOl());
        ctx.setInOl(true);
        assertTrue(ctx.isInOl());
        ctx.setInOl(false);
        assertFalse(ctx.isInOl());
    }

    @Test
    @DisplayName("Unordered list flag getter/setter round trip")
    void testInUlGetterSetter() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        assertFalse(ctx.isInUl());
        ctx.setInUl(true);
        assertTrue(ctx.isInUl());
        ctx.setInUl(false);
        assertFalse(ctx.isInUl());
    }

    @Test
    @DisplayName("Alpha ordered list flag getter/setter round trip")
    void testInOlAlphaGetterSetter() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        assertFalse(ctx.isInOlAlpha());
        ctx.setInOlAlpha(true);
        assertTrue(ctx.isInOlAlpha());
        ctx.setInOlAlpha(false);
        assertFalse(ctx.isInOlAlpha());
    }

    @Test
    @DisplayName("List item flag getter/setter round trip")
    void testInListItemGetterSetter() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();
        assertFalse(ctx.isInListItem());
        ctx.setInListItem(true);
        assertTrue(ctx.isInListItem());
        ctx.setInListItem(false);
        assertFalse(ctx.isInListItem());
    }

    @Test
    @DisplayName("Simulated paragraph-then-list workflow produces expected markup")
    void testRealisticWorkflow() {
        HtmlGenerationContext ctx = new HtmlGenerationContext();

        ctx.append("<p>");
        ctx.setInParagraph(true);
        ctx.append("Some text");

        // empty line closes paragraph
        ctx.handleEmptyLine();

        ctx.append("<ul>\n");
        ctx.setInUl(true);
        ctx.append("  <li>item one");
        ctx.setInListItem(true);

        // end of document - close everything still open
        ctx.closeAll();

        String expected = "<p>Some text</p>\n<ul>\n  <li>item one</li>\n    </ul>\n";
        assertEquals(expected, ctx.getHtml());
    }
}
