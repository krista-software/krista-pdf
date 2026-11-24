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
 * Tests for {@link HeadingHandler}.
 * Verifies the canHandle predicate and the exact HTML produced for every
 * combination of open-structure flags that {@link HeadingHandler#handle} must close.
 */
@DisplayName("HeadingHandler Tests")
class HeadingHandlerTest {

    private HeadingHandler handler;
    private HtmlGenerationContext ctx;

    @BeforeEach
    void setUp() {
        handler = new HeadingHandler();
        ctx = new HtmlGenerationContext();
    }

    @Test
    @DisplayName("canHandle returns true only for HEADING")
    void testCanHandle() {
        assertTrue(handler.canHandle(LineType.HEADING));
        assertFalse(handler.canHandle(LineType.PARAGRAPH));
        assertFalse(handler.canHandle(LineType.ORDERED_ITEM));
        assertFalse(handler.canHandle(LineType.ORDERED_ITEM_ALPHA));
        assertFalse(handler.canHandle(LineType.BULLET_ITEM));
    }

    @Test
    @DisplayName("handle on a fresh context renders a plain h2 with no closing tags")
    void testHandle_FreshContext() {
        handler.handle("Overview", ctx);

        assertEquals("    <h2>Overview</h2>\n", ctx.getHtml());
        assertFalse(ctx.isInListItem());
        assertFalse(ctx.isInParagraph());
        assertFalse(ctx.isInOl());
        assertFalse(ctx.isInUl());
    }

    @Test
    @DisplayName("handle closes an open list item before rendering the heading")
    void testHandle_ClosesOpenListItem() {
        ctx.setInListItem(true);

        handler.handle("Overview", ctx);

        assertEquals("</li>\n    <h2>Overview</h2>\n", ctx.getHtml());
        assertFalse(ctx.isInListItem());
    }

    @Test
    @DisplayName("handle closes an open paragraph before rendering the heading")
    void testHandle_ClosesOpenParagraph() {
        ctx.setInParagraph(true);

        handler.handle("Overview", ctx);

        assertEquals("</p>\n    <h2>Overview</h2>\n", ctx.getHtml());
        assertFalse(ctx.isInParagraph());
    }

    @Test
    @DisplayName("handle closes an open numeric ordered list before rendering the heading")
    void testHandle_ClosesOpenOl() {
        ctx.setInOl(true);

        handler.handle("Overview", ctx);

        assertEquals("    </ol>\n    <h2>Overview</h2>\n", ctx.getHtml());
        assertFalse(ctx.isInOl());
    }

    @Test
    @DisplayName("handle closes an open unordered list before rendering the heading")
    void testHandle_ClosesOpenUl() {
        ctx.setInUl(true);

        handler.handle("Overview", ctx);

        assertEquals("    </ul>\n    <h2>Overview</h2>\n", ctx.getHtml());
        assertFalse(ctx.isInUl());
    }

    @Test
    @DisplayName("handle closes every open structure in order: li, p, ol, ul")
    void testHandle_ClosesAllOpenStructures() {
        ctx.setInListItem(true);
        ctx.setInParagraph(true);
        ctx.setInOl(true);
        ctx.setInUl(true);

        handler.handle("Overview", ctx);

        assertEquals("</li>\n</p>\n    </ol>\n    </ul>\n    <h2>Overview</h2>\n", ctx.getHtml());
        assertFalse(ctx.isInListItem());
        assertFalse(ctx.isInParagraph());
        assertFalse(ctx.isInOl());
        assertFalse(ctx.isInUl());
    }

    @Test
    @DisplayName("handle escapes HTML special characters in the heading text")
    void testHandle_EscapesSpecialCharacters() {
        handler.handle("<Title> & \"Quoted\" 'Test'", ctx);

        assertEquals("    <h2>&lt;Title&gt; &amp; &quot;Quoted&quot; &#39;Test&#39;</h2>\n", ctx.getHtml());
    }

    @Test
    @DisplayName("handle on an empty line still renders empty h2 tags")
    void testHandle_EmptyLine() {
        handler.handle("", ctx);

        assertEquals("    <h2></h2>\n", ctx.getHtml());
    }

    @Test
    @DisplayName("Mockito: handle invokes append and the state setters in the expected order")
    void testHandle_InteractionOrderWithMock() {
        HtmlGenerationContext mockCtx = mock(HtmlGenerationContext.class);
        when(mockCtx.isInListItem()).thenReturn(true);
        when(mockCtx.isInParagraph()).thenReturn(true);
        when(mockCtx.isInOl()).thenReturn(true);
        when(mockCtx.isInUl()).thenReturn(true);
        when(mockCtx.append(anyString())).thenReturn(mockCtx);

        handler.handle("Section", mockCtx);

        InOrder inOrder = inOrder(mockCtx);
        inOrder.verify(mockCtx).append("</li>\n");
        inOrder.verify(mockCtx).setInListItem(false);
        inOrder.verify(mockCtx).append("</p>\n");
        inOrder.verify(mockCtx).setInParagraph(false);
        inOrder.verify(mockCtx).append("    </ol>\n");
        inOrder.verify(mockCtx).setInOl(false);
        inOrder.verify(mockCtx).append("    </ul>\n");
        inOrder.verify(mockCtx).setInUl(false);
        inOrder.verify(mockCtx).append("    <h2>");
        inOrder.verify(mockCtx).append("Section");
        inOrder.verify(mockCtx).append("</h2>\n");
    }
}
