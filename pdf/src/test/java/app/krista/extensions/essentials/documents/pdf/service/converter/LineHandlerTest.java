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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Contract tests for the {@link LineHandler} strategy interface.
 * Since the interface declares no default behavior, these tests verify (a) a
 * minimal custom implementation satisfies the contract, (b) a Mockito mock can
 * stand in for the interface, and (c) every concrete production handler
 * ({@link HeadingHandler}, {@link OrderedItemHandler}, {@link OrderedAlphaItemHandler},
 * {@link BulletItemHandler}, {@link ParagraphHandler}) is usable polymorphically
 * through the {@code LineHandler} reference type exactly as
 * {@link HtmlTextProcessor} relies on.
 */
@DisplayName("LineHandler Contract Tests")
class LineHandlerTest {

    @Test
    @DisplayName("A custom LineHandler implementation can be invoked through the interface type")
    void testCustomImplementation_CanHandleAndHandle() {
        LineHandler handler = new LineHandler() {
            @Override
            public boolean canHandle(LineType type) {
                return type == LineType.PARAGRAPH;
            }

            @Override
            public void handle(String line, HtmlGenerationContext context) {
                context.append("[[" + line + "]]");
            }
        };

        assertTrue(handler.canHandle(LineType.PARAGRAPH));
        assertFalse(handler.canHandle(LineType.HEADING));

        HtmlGenerationContext ctx = new HtmlGenerationContext();
        handler.handle("hello", ctx);
        assertEquals("[[hello]]", ctx.getHtml());
    }

    @Test
    @DisplayName("A Mockito mock satisfies the LineHandler contract and records interactions")
    void testMockImplementation_RecordsInteractions() {
        LineHandler mockHandler = mock(LineHandler.class);
        when(mockHandler.canHandle(LineType.BULLET_ITEM)).thenReturn(true);

        HtmlGenerationContext ctx = new HtmlGenerationContext();
        assertTrue(mockHandler.canHandle(LineType.BULLET_ITEM));
        mockHandler.handle("- item", ctx);

        verify(mockHandler).canHandle(LineType.BULLET_ITEM);
        verify(mockHandler).handle("- item", ctx);
    }

    @Test
    @DisplayName("Every production LineHandler reports canHandle() for exactly its own LineType")
    void testProductionHandlers_EachOwnsExactlyOneLineType() {
        List<LineHandler> handlers = Arrays.asList(
                new HeadingHandler(),
                new OrderedItemHandler(),
                new OrderedAlphaItemHandler(),
                new BulletItemHandler(),
                new ParagraphHandler());

        for (LineType type : LineType.values()) {
            long matchCount = handlers.stream().filter(h -> h.canHandle(type)).count();
            assertEquals(1, matchCount, "Exactly one handler should claim LineType " + type);
        }
    }

    @Test
    @DisplayName("Dispatch loop pattern (as used by HtmlTextProcessor) routes each LineType to the correct handler")
    void testDispatchLoopPattern_RoutesToCorrectHandler() {
        List<LineHandler> handlers = Arrays.asList(
                new HeadingHandler(),
                new OrderedItemHandler(),
                new OrderedAlphaItemHandler(),
                new BulletItemHandler(),
                new ParagraphHandler());

        HtmlGenerationContext ctx = new HtmlGenerationContext();
        LineHandler selected = null;
        for (LineHandler handler : handlers) {
            if (handler.canHandle(LineType.ORDERED_ITEM_ALPHA)) {
                selected = handler;
                break;
            }
        }

        assertNotNull(selected);
        assertTrue(selected instanceof OrderedAlphaItemHandler);

        selected.handle("a. Apple", ctx);
        assertEquals("\n        <ol type=\"a\">\n          <li>Apple", ctx.getHtml());
    }
}
