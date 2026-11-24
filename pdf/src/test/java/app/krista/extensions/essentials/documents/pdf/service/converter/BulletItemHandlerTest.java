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
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link BulletItemHandler}.
 * Covers the canHandle predicate and every closing/opening branch of handle():
 * closing an open alphabetical list (with and without a dangling item),
 * closing a previous bullet item, closing an open paragraph, closing an open
 * numeric ordered list, and opening the unordered list only when needed.
 */
@DisplayName("BulletItemHandler Tests")
class BulletItemHandlerTest {

    private BulletItemHandler handler;
    private HtmlGenerationContext ctx;

    @BeforeEach
    void setUp() {
        handler = new BulletItemHandler();
        ctx = new HtmlGenerationContext();
    }

    @Test
    @DisplayName("canHandle returns true only for BULLET_ITEM")
    void testCanHandle() {
        assertTrue(handler.canHandle(LineType.BULLET_ITEM));
        assertFalse(handler.canHandle(LineType.ORDERED_ITEM));
        assertFalse(handler.canHandle(LineType.ORDERED_ITEM_ALPHA));
        assertFalse(handler.canHandle(LineType.HEADING));
        assertFalse(handler.canHandle(LineType.PARAGRAPH));
    }

    @Test
    @DisplayName("handle on a fresh context opens <ul> and appends the first <li>")
    void testHandle_FreshContext_OpensUl() {
        handler.handle("- First", ctx);

        assertEquals("    <ul>\n      <li>First", ctx.getHtml());
        assertTrue(ctx.isInUl());
        assertTrue(ctx.isInListItem());
    }

    @Test
    @DisplayName("handle recognizes the '*' and bullet-dot markers too")
    void testHandle_AlternateMarkers() {
        handler.handle("* Starred", ctx);
        assertEquals("    <ul>\n      <li>Starred", ctx.getHtml());

        HtmlGenerationContext ctx2 = new HtmlGenerationContext();
        handler.handle("• Dotted", ctx2);
        assertEquals("    <ul>\n      <li>Dotted", ctx2.getHtml());
    }

    @Test
    @DisplayName("handle does not reopen <ul> if it is already open")
    void testHandle_DoesNotReopenUl() {
        ctx.setInUl(true);

        handler.handle("- Second", ctx);

        assertEquals("      <li>Second", ctx.getHtml());
        assertTrue(ctx.isInUl());
    }

    @Test
    @DisplayName("handle closes a previously open list item before appending the next one")
    void testHandle_ClosesPreviousListItem() {
        ctx.setInUl(true);
        ctx.setInListItem(true);

        handler.handle("- Third", ctx);

        assertEquals("</li>\n      <li>Third", ctx.getHtml());
        assertTrue(ctx.isInListItem());
    }

    @Test
    @DisplayName("handle closes an open paragraph before opening the list")
    void testHandle_ClosesOpenParagraph() {
        ctx.setInParagraph(true);

        handler.handle("- Item", ctx);

        assertEquals("</p>\n    <ul>\n      <li>Item", ctx.getHtml());
        assertFalse(ctx.isInParagraph());
    }

    @Test
    @DisplayName("handle closes an open numeric ordered list before opening the unordered list")
    void testHandle_ClosesOpenOl() {
        ctx.setInOl(true);

        handler.handle("- Item", ctx);

        assertEquals("    </ol>\n    <ul>\n      <li>Item", ctx.getHtml());
        assertFalse(ctx.isInOl());
    }

    @Test
    @DisplayName("handle closes an open alphabetical list and its open item, then opens a bullet list")
    void testHandle_ClosesAlphaListWithOpenItem() {
        ctx.setInOlAlpha(true);
        ctx.setInListItem(true);

        handler.handle("- Item", ctx);

        assertEquals("</li>\n      </ol>\n    <ul>\n      <li>Item", ctx.getHtml());
        assertFalse(ctx.isInOlAlpha());
        assertTrue(ctx.isInUl());
    }

    @Test
    @DisplayName("handle closes an open alphabetical list without a dangling item close when no item is open")
    void testHandle_ClosesAlphaListWithoutOpenItem() {
        ctx.setInOlAlpha(true);

        handler.handle("- Item", ctx);

        assertEquals("      </ol>\n    <ul>\n      <li>Item", ctx.getHtml());
        assertFalse(ctx.isInOlAlpha());
    }

    @Test
    @DisplayName("handle escapes HTML special characters in the item text")
    void testHandle_EscapesSpecialCharacters() {
        handler.handle("- <b>Bold</b> & 'quoted'", ctx);

        assertEquals("    <ul>\n      <li>&lt;b&gt;Bold&lt;/b&gt; &amp; &#39;quoted&#39;", ctx.getHtml());
    }

    @Test
    @DisplayName("handle closes every open structure in the documented order")
    void testHandle_ClosesEverythingInOrder() {
        ctx.setInOlAlpha(true);
        ctx.setInListItem(true);
        ctx.setInParagraph(true);
        ctx.setInOl(true);

        handler.handle("- Item", ctx);

        assertEquals("</li>\n      </ol>\n</p>\n    </ol>\n    <ul>\n      <li>Item", ctx.getHtml());
        assertFalse(ctx.isInOlAlpha());
        assertFalse(ctx.isInParagraph());
        assertFalse(ctx.isInOl());
        assertTrue(ctx.isInUl());
        assertTrue(ctx.isInListItem());
    }

    @Test
    @DisplayName("Mockito: handle invokes append and setters in the expected order")
    void testHandle_InteractionOrderWithMock() {
        HtmlGenerationContext mockCtx = mock(HtmlGenerationContext.class);
        when(mockCtx.isInOlAlpha()).thenReturn(false);
        when(mockCtx.isInListItem()).thenReturn(false);
        when(mockCtx.isInParagraph()).thenReturn(false);
        when(mockCtx.isInOl()).thenReturn(false);
        when(mockCtx.isInUl()).thenReturn(false);
        when(mockCtx.append(anyString())).thenReturn(mockCtx);

        handler.handle("- Item", mockCtx);

        InOrder inOrder = inOrder(mockCtx);
        inOrder.verify(mockCtx).append("    <ul>\n");
        inOrder.verify(mockCtx).setInUl(true);
        inOrder.verify(mockCtx).append("      <li>");
        inOrder.verify(mockCtx).append("Item");
        inOrder.verify(mockCtx).setInListItem(true);
    }
}
