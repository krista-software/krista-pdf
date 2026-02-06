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

/**
 * Handles regular paragraph text and converts it to HTML {@code <p>} elements.
 * Also handles continuation of text within list items.
 */
public class ParagraphHandler implements LineHandler {

    /**
     * Constructs a new ParagraphHandler.
     */
    public ParagraphHandler() {
        // Default constructor
    }

    /**
     * Checks if this handler can process paragraph text.
     *
     * @param type the line type to check
     * @return true if type is PARAGRAPH
     */
    @Override
    public boolean canHandle(LineType type) {
        return type == LineType.PARAGRAPH;
    }

    /**
     * Converts paragraph text to HTML.
     * If inside a list item, appends text to the current item.
     * Otherwise, closes any open lists and opens or continues a paragraph.
     *
     * @param line the paragraph text line
     * @param ctx the HTML generation context
     */
    @Override
    public void handle(String line, HtmlGenerationContext ctx) {
        if (ctx.isInListItem()) {
            // Continue text within list item
            ctx.append(" ").append(escapeHtml(line));
        } else {
            // Close any open lists
            if (ctx.isInOl()) {
                ctx.append("    </ol>\n");
                ctx.setInOl(false);
            }
            if (ctx.isInUl()) {
                ctx.append("    </ul>\n");
                ctx.setInUl(false);
            }
            // Open paragraph if not already open, otherwise continue
            if (!ctx.isInParagraph()) {
                ctx.append("    <p>");
                ctx.setInParagraph(true);
            } else {
                ctx.append(" ");
            }
            ctx.append(escapeHtml(line));
        }
    }
}
