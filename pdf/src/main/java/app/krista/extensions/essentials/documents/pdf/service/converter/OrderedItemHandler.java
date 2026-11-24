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

import static app.krista.extensions.essentials.documents.pdf.service.converter.HtmlTextProcessor.escapeHtml;
import static app.krista.extensions.essentials.documents.pdf.service.converter.HtmlTextProcessor.removeListMarker;

/**
 * Handles numeric ordered list items and converts them to HTML {@code <ol>} elements.
 * Processes lines like "1. Item", "2) Item", etc.
 */
public class OrderedItemHandler implements LineHandler {

    /**
     * Constructs a new OrderedItemHandler.
     */
    public OrderedItemHandler() {
        // Default constructor
    }

    /**
     * Checks if this handler can process numeric ordered list items.
     *
     * @param type the line type to check
     * @return true if type is ORDERED_ITEM
     */
    @Override
    public boolean canHandle(LineType type) {
        return type == LineType.ORDERED_ITEM;
    }

    /**
     * Converts a numeric list item to HTML.
     * Closes any open alphabetical lists, unordered lists, paragraphs, or list items,
     * opens a numeric ordered list if needed, and appends the list item.
     *
     * @param line the numeric list item line
     * @param ctx the HTML generation context
     */
    @Override
    public void handle(String line, HtmlGenerationContext ctx) {
        // Close alphabetical ordered list if open
        if (ctx.isInOlAlpha()) {
            if (ctx.isInListItem()) {
                ctx.append("</li>\n");
                ctx.setInListItem(false);
            }
            ctx.append("      </ol>\n");
            ctx.setInOlAlpha(false);
        }
        // Close previous list item
        if (ctx.isInListItem()) {
            ctx.append("</li>\n");
            ctx.setInListItem(false);
        }
        // Close paragraph if open
        if (ctx.isInParagraph()) {
            ctx.append("</p>\n");
            ctx.setInParagraph(false);
        }
        // Close unordered list if open
        if (ctx.isInUl()) {
            ctx.append("    </ul>\n");
            ctx.setInUl(false);
        }
        // Open numeric ordered list if not already open
        if (!ctx.isInOl()) {
            ctx.append("    <ol>\n");
            ctx.setInOl(true);
        }
        // Append list item with escaped content
        ctx.append("      <li>").append(escapeHtml(removeListMarker(line)));
        ctx.setInListItem(true);
    }
}
