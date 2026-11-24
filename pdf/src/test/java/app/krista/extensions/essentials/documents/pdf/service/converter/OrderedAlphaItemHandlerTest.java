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
 * Tests for {@link OrderedAlphaItemHandler}.
 * Exercises the canHandle predicate and the compound
 * {@code isInListItem() && isInOlAlpha()} condition in handle() across all
 * four truth-table combinations, plus the paragraph-close and list-open branches.
 */
@DisplayName("OrderedAlphaItemHandler Tests")
class OrderedAlphaItemHandlerTest {

    private OrderedAlphaItemHandler handler;
    private HtmlGenerationContext ctx;

    @BeforeEach
    void setUp() {
        handler = new OrderedAlphaItemHandler();
        ctx = new HtmlGenerationContext();
    }

    @Test
    @DisplayName("canHandle returns true only for ORDERED_ITEM_ALPHA")
    void testCanHandle() {
        assertTrue(handler.canHandle(LineType.ORDERED_ITEM_ALPHA));
        assertFalse(handler.canHandle(LineType.ORDERED_ITEM));
        assertFalse(handler.canHandle(LineType.BULLET_ITEM));
        assertFalse(handler.canHandle(LineType.HEADING));
        assertFalse(handler.canHandle(LineType.PARAGRAPH));
    }

    @Test
    @DisplayName("handle on a fresh context (listItem=false, olAlpha=false) opens the alpha list")
    void testHandle_BothFalse_OpensAlphaList() {
        handler.handle("a. Apple", ctx);

        assertEquals("\n        <ol type=\"a\">\n          <li>Apple", ctx.getHtml());
        assertTrue(ctx.isInOlAlpha());
        assertTrue(ctx.isInListItem());
    }

    @Test
    @DisplayName("handle strips a ')' alphabetical marker too")
    void testHandle_ParenMarker() {
        handler.handle("b) Banana", ctx);

        assertEquals("\n        <ol type=\"a\">\n          <li>Banana", ctx.getHtml());
    }

    @Test
    @DisplayName("handle with listItem=true and olAlpha=true closes the previous item and does not reopen the list")
    void testHandle_BothTrue_ClosesItemOnly() {
        ctx.setInListItem(true);
        ctx.setInOlAlpha(true);

        handler.handle("b. Banana", ctx);

        assertEquals("</li>\n          <li>Banana", ctx.getHtml());
        assertTrue(ctx.isInOlAlpha());
        assertTrue(ctx.isInListItem());
    }

    @Test
    @DisplayName("handle with listItem=true but olAlpha=false does NOT close the dangling item (AND short-circuit)")
    void testHandle_ListItemTrueOlAlphaFalse_SkipsClose() {
        ctx.setInListItem(true);

        handler.handle("a. Apple", ctx);

        // The && condition requires BOTH flags true; since isInOlAlpha() is false,
        // the previous <li> is never closed before the new list is opened.
        assertEquals("\n        <ol type=\"a\">\n          <li>Apple", ctx.getHtml());
        assertTrue(ctx.isInOlAlpha());
        assertTrue(ctx.isInListItem());
    }

    @Test
    @DisplayName("handle with listItem=false and olAlpha=true does not close any item and does not reopen the list")
    void testHandle_ListItemFalseOlAlphaTrue_SkipsCloseAndReopen() {
        ctx.setInOlAlpha(true);

        handler.handle("a. Apple", ctx);

        assertEquals("          <li>Apple", ctx.getHtml());
        assertTrue(ctx.isInOlAlpha());
        assertTrue(ctx.isInListItem());
    }

    @Test
    @DisplayName("handle closes an open paragraph before opening the alpha list")
    void testHandle_ClosesOpenParagraph() {
        ctx.setInParagraph(true);

        handler.handle("a. Apple", ctx);

        assertEquals("</p>\n\n        <ol type=\"a\">\n          <li>Apple", ctx.getHtml());
        assertFalse(ctx.isInParagraph());
    }

    @Test
    @DisplayName("handle escapes HTML special characters in the item text")
    void testHandle_EscapesSpecialCharacters() {
        handler.handle("a. <i>Italic</i> & 'quoted'", ctx);

        assertEquals("\n        <ol type=\"a\">\n          <li>&lt;i&gt;Italic&lt;/i&gt; &amp; &#39;quoted&#39;",
                ctx.getHtml());
    }

    @Test
    @DisplayName("handle two sequential items produces a well-formed two-item list")
    void testHandle_TwoSequentialItems() {
        handler.handle("a. Apple", ctx);
        handler.handle("b. Banana", ctx);

        assertEquals("\n        <ol type=\"a\">\n          <li>Apple</li>\n          <li>Banana", ctx.getHtml());
    }

    @Test
    @DisplayName("Mockito: handle invokes append and setters in the expected order")
    void testHandle_InteractionOrderWithMock() {
        HtmlGenerationContext mockCtx = mock(HtmlGenerationContext.class);
        when(mockCtx.isInListItem()).thenReturn(false);
        when(mockCtx.isInOlAlpha()).thenReturn(false);
        when(mockCtx.isInParagraph()).thenReturn(false);
        when(mockCtx.append(anyString())).thenReturn(mockCtx);

        handler.handle("a. Item", mockCtx);

        InOrder inOrder = inOrder(mockCtx);
        inOrder.verify(mockCtx).append("\n        <ol type=\"a\">\n");
        inOrder.verify(mockCtx).setInOlAlpha(true);
        inOrder.verify(mockCtx).append("          <li>");
        inOrder.verify(mockCtx).append("Item");
        inOrder.verify(mockCtx).setInListItem(true);
    }
}
