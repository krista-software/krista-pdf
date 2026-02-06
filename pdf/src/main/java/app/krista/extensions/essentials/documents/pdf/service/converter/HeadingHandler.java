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
 * Handles conversion of heading lines to HTML {@code <h2>} elements.
 * Closes any open lists or paragraphs before rendering the heading.
 */
public class HeadingHandler implements LineHandler {

    /**
     * Constructs a new HeadingHandler.
     */
    public HeadingHandler() {
        // Default constructor
    }

    /**
     * Checks if this handler processes heading lines.
     *
     * @param type the line type to check
     * @return true if type is HEADING
     */
    @Override
    public boolean canHandle(LineType type) {
        return type == LineType.HEADING;
    }

    /**
     * Converts a heading line to an HTML {@code <h2>} element.
     * Closes any open list items, paragraphs, or lists before rendering.
     *
     * @param line the heading text to convert
     * @param ctx the HTML generation context
     */
    @Override
    public void handle(String line, HtmlGenerationContext ctx) {
        // Close any open structures
        if (ctx.isInListItem()) {
            ctx.append("</li>\n");
            ctx.setInListItem(false);
        }
        if (ctx.isInParagraph()) {
            ctx.append("</p>\n");
            ctx.setInParagraph(false);
        }
        if (ctx.isInOl()) {
            ctx.append("    </ol>\n");
            ctx.setInOl(false);
        }
        if (ctx.isInUl()) {
            ctx.append("    </ul>\n");
            ctx.setInUl(false);
        }

        // Render heading with escaped content
        ctx.append("    <h2>").append(escapeHtml(line)).append("</h2>\n");
    }
}
