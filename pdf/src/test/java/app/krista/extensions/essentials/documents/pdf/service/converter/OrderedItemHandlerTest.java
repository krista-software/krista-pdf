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
 * Tests for {@link OrderedItemHandler}.
 * Covers the canHandle predicate and every branch of handle(): closing a
 * previously open alphabetical list, closing a previous numbered item,
 * closing an open paragraph/unordered list, and opening the numbered list
 * only when it isn't already open.
 */
@DisplayName("OrderedItemHandler Tests")
class OrderedItemHandlerTest {

    private OrderedItemHandler handler;
    private HtmlGenerationContext ctx;

    @BeforeEach
    void setUp() {
        handler = new OrderedItemHandler();
        ctx = new HtmlGenerationContext();
    }

    @Test
    @DisplayName("canHandle returns true only for ORDERED_ITEM")
    void testCanHandle() {
        assertTrue(handler.canHandle(LineType.ORDERED_ITEM));
        assertFalse(handler.canHandle(LineType.ORDERED_ITEM_ALPHA));
        assertFalse(handler.canHandle(LineType.BULLET_ITEM));
        assertFalse(handler.canHandle(LineType.HEADING));
        assertFalse(handler.canHandle(LineType.PARAGRAPH));
    }

    @Test
    @DisplayName("handle on a fresh context opens <ol> and appends the first <li>")
    void testHandle_FreshContext_OpensOl() {
        handler.handle("1. First item", ctx);

        assertEquals("    <ol>\n      <li>First item", ctx.getHtml());
        assertTrue(ctx.isInOl());
        assertTrue(ctx.isInListItem());
    }

    @Test
    @DisplayName("handle strips a numeric marker using ')' delimiter too")
    void testHandle_ParenMarker() {
        handler.handle("2) Second item", ctx);

        assertEquals("    <ol>\n      <li>Second item", ctx.getHtml());
    }

    @Test
    @DisplayName("handle does not reopen <ol> if it is already open")
    void testHandle_DoesNotReopenOl() {
        ctx.setInOl(true);

        handler.handle("2. Second item", ctx);

        assertEquals("      <li>Second item", ctx.getHtml());
        assertTrue(ctx.isInOl());
    }

    @Test
    @DisplayName("handle closes a previously open list item before appending the next one")
    void testHandle_ClosesPreviousListItem() {
        ctx.setInOl(true);
        ctx.setInListItem(true);

        handler.handle("3. Third item", ctx);

        assertEquals("</li>\n      <li>Third item", ctx.getHtml());
        assertTrue(ctx.isInListItem());
    }

    @Test
    @DisplayName("handle closes an open paragraph before opening the list")
    void testHandle_ClosesOpenParagraph() {
        ctx.setInParagraph(true);

        handler.handle("1. Item", ctx);

        assertEquals("</p>\n    <ol>\n      <li>Item", ctx.getHtml());
        assertFalse(ctx.isInParagraph());
    }

    @Test
    @DisplayName("handle closes an open unordered list before opening the ordered list")
    void testHandle_ClosesOpenUl() {
        ctx.setInUl(true);

        handler.handle("1. Item", ctx);

        assertEquals("    </ul>\n    <ol>\n      <li>Item", ctx.getHtml());
        assertFalse(ctx.isInUl());
    }

    @Test
    @DisplayName("handle closes an open alphabetical list and its open item, then opens a numeric list")
    void testHandle_ClosesAlphaListWithOpenItem() {
        ctx.setInOlAlpha(true);
        ctx.setInListItem(true);

        handler.handle("1. Item", ctx);

        assertEquals("</li>\n      </ol>\n    <ol>\n      <li>Item", ctx.getHtml());
        assertFalse(ctx.isInOlAlpha());
        assertTrue(ctx.isInOl());
        assertTrue(ctx.isInListItem());
    }

    @Test
    @DisplayName("handle closes an open alphabetical list without a dangling item close when no item is open")
    void testHandle_ClosesAlphaListWithoutOpenItem() {
        ctx.setInOlAlpha(true);

        handler.handle("1. Item", ctx);

        assertEquals("      </ol>\n    <ol>\n      <li>Item", ctx.getHtml());
        assertFalse(ctx.isInOlAlpha());
    }

    @Test
    @DisplayName("handle escapes HTML special characters in the item text")
    void testHandle_EscapesSpecialCharacters() {
        handler.handle("1. <b>Bold</b> & 'quoted'", ctx);

        assertEquals("    <ol>\n      <li>&lt;b&gt;Bold&lt;/b&gt; &amp; &#39;quoted&#39;", ctx.getHtml());
    }

    @Test
    @DisplayName("handle closes every open structure in the documented order")
    void testHandle_ClosesEverythingInOrder() {
        ctx.setInOlAlpha(true);
        ctx.setInListItem(true);
        ctx.setInParagraph(true);
        ctx.setInUl(true);

        handler.handle("1. Item", ctx);

        assertEquals("</li>\n      </ol>\n</p>\n    </ul>\n    <ol>\n      <li>Item", ctx.getHtml());
        assertFalse(ctx.isInOlAlpha());
        assertFalse(ctx.isInParagraph());
        assertFalse(ctx.isInUl());
        assertTrue(ctx.isInOl());
        assertTrue(ctx.isInListItem());
    }

    @Test
    @DisplayName("Mockito: handle invokes append and setters in the expected order")
    void testHandle_InteractionOrderWithMock() {
        HtmlGenerationContext mockCtx = mock(HtmlGenerationContext.class);
        when(mockCtx.isInOlAlpha()).thenReturn(false);
        when(mockCtx.isInListItem()).thenReturn(false);
        when(mockCtx.isInParagraph()).thenReturn(false);
        when(mockCtx.isInUl()).thenReturn(false);
        when(mockCtx.isInOl()).thenReturn(false);
        when(mockCtx.append(anyString())).thenReturn(mockCtx);

        handler.handle("1. Item", mockCtx);

        InOrder inOrder = inOrder(mockCtx);
        inOrder.verify(mockCtx).append("    <ol>\n");
        inOrder.verify(mockCtx).setInOl(true);
        inOrder.verify(mockCtx).append("      <li>");
        inOrder.verify(mockCtx).append("Item");
        inOrder.verify(mockCtx).setInListItem(true);
    }
}
