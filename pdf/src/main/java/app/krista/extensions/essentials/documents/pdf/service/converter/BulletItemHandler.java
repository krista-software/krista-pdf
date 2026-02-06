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
 * Handler for processing bullet list items and converting them to HTML unordered list elements.
 *
 * <p>This handler is responsible for detecting and converting bullet-point list items
 * (lines starting with markers like •, -, *, etc.) into proper HTML {@code <ul>} and
 * {@code <li>} elements. It manages the state transitions between different HTML
 * structures (paragraphs, ordered lists, unordered lists) to ensure valid HTML output.</p>
 *
 * <p><strong>Supported Bullet Markers:</strong></p>
 * <ul>
 *   <li>• (bullet point)</li>
 *   <li>- (hyphen)</li>
 *   <li>* (asterisk)</li>
 *   <li>Other markers detected by {@link HtmlTextProcessor#removeListMarker(String)}</li>
 * </ul>
 *
 * <p><strong>HTML Structure Generated:</strong></p>
 * <pre>{@code
 * <ul>
 *   <li>First bullet item
 *   <li>Second bullet item
 *   <li>Third bullet item
 * </ul>
 * }</pre>
 *
 * <p><strong>State Management:</strong></p>
 * <p>This handler manages complex state transitions to ensure proper HTML nesting:</p>
 * <ul>
 *   <li>Closes any open alphabetical ordered lists ({@code <ol>})</li>
 *   <li>Closes any open list items ({@code <li>})</li>
 *   <li>Closes any open paragraphs ({@code <p>})</li>
 *   <li>Closes any open numeric ordered lists ({@code <ol>})</li>
 *   <li>Opens a new unordered list ({@code <ul>}) if not already open</li>
 *   <li>Opens a new list item ({@code <li>}) for the current bullet</li>
 * </ul>
 */
public class BulletItemHandler implements LineHandler {

    /**
     * Constructs a new BulletItemHandler.
     *
     * <p>This handler is stateless and can be reused across multiple HTML generation
     * operations. All state is maintained in the {@link HtmlGenerationContext} passed
     * to the {@link #handle(String, HtmlGenerationContext)} method.</p>
     */
    public BulletItemHandler() {
        // Default constructor - handler is stateless
    }

    /**
     * Determines if this handler can process the given line type.
     *
     * <p>This handler only processes lines identified as {@link LineType#BULLET_ITEM},
     * which are lines starting with bullet markers (•, -, *, etc.).</p>
     *
     * @param type the type of line to check
     * @return {@code true} if the type is {@link LineType#BULLET_ITEM}, {@code false} otherwise
     * @see LineType#BULLET_ITEM
     */
    @Override
    public boolean canHandle(LineType type) {
        return type == LineType.BULLET_ITEM;
    }

    /**
     * Processes a bullet list item line and converts it to HTML unordered list markup.
     *
     * <p>This method performs the following operations in sequence:</p>
     * <ol>
     *   <li><strong>Close alphabetical ordered list:</strong> If currently in an alphabetical
     *       ordered list ({@code <ol>}), closes any open list item and the list itself</li>
     *   <li><strong>Close previous list item:</strong> If a list item is open from a previous
     *       bullet, closes it with {@code </li>}</li>
     *   <li><strong>Close paragraph:</strong> If currently in a paragraph, closes it with
     *       {@code </p>} to maintain proper HTML structure</li>
     *   <li><strong>Close numeric ordered list:</strong> If currently in a numeric ordered
     *       list, closes it with {@code </ol>}</li>
     *   <li><strong>Open unordered list:</strong> If not already in an unordered list,
     *       opens a new {@code <ul>} element</li>
     *   <li><strong>Create list item:</strong> Removes the bullet marker from the line,
     *       escapes HTML special characters, and wraps the content in {@code <li>} tags</li>
     * </ol>
     *
     * <p><strong>Example Transformation:</strong></p>
     * <pre>
     * Input:  "• First item"
     * Output: "&lt;ul&gt;\n  &lt;li&gt;First item"
     *
     * Input:  "- Second item"
     * Output: "  &lt;li&gt;Second item"
     * </pre>
     *
     * <p><strong>State Transitions:</strong></p>
     * <ul>
     *   <li>{@code isInOlAlpha} → {@code false} (closes alphabetical ordered list)</li>
     *   <li>{@code isInListItem} → {@code false} → {@code true} (closes previous, opens new)</li>
     *   <li>{@code isInParagraph} → {@code false} (closes paragraph)</li>
     *   <li>{@code isInOl} → {@code false} (closes numeric ordered list)</li>
     *   <li>{@code isInUl} → {@code true} (ensures unordered list is open)</li>
     * </ul>
     *
     * <p><strong>HTML Safety:</strong> All text content is HTML-escaped using
     * {@link HtmlTextProcessor#escapeHtml(String)} to prevent XSS vulnerabilities.</p>
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

        // Close previous list item if open
        if (ctx.isInListItem()) {
            ctx.append("</li>\n");
            ctx.setInListItem(false);
        }

        // Close paragraph if open
        if (ctx.isInParagraph()) {
            ctx.append("</p>\n");
            ctx.setInParagraph(false);
        }

        // Close numeric ordered list if open
        if (ctx.isInOl()) {
            ctx.append("    </ol>\n");
            ctx.setInOl(false);
        }

        // Open unordered list if not already open
        if (!ctx.isInUl()) {
            ctx.append("    <ul>\n");
            ctx.setInUl(true);
        }

        // Create list item with escaped content (marker removed)
        ctx.append("      <li>").append(escapeHtml(removeListMarker(line)));
        ctx.setInListItem(true);
    }
}
