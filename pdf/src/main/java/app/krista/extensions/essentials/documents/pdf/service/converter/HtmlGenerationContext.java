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

/**
 * Context for managing HTML generation state during PDF-to-HTML conversion.
 * Tracks open HTML elements and builds the output HTML string.
 */
public class HtmlGenerationContext {
    private final StringBuilder html = new StringBuilder();
    boolean inParagraph = false;
    boolean inOl = false;
    boolean inUl = false;
    boolean inOlAlpha = false;
    boolean inListItem = false;

    /**
     * Constructs a new HtmlGenerationContext with empty state.
     */
    public HtmlGenerationContext() {
        // Initialize with default state
    }

    /**
     * Appends HTML content to the output.
     *
     * @param s the HTML string to append
     * @return this context for method chaining
     */
    public HtmlGenerationContext append(String s) {
        html.append(s);
        return this;
    }

    /**
     * Returns the generated HTML content.
     *
     * @return the complete HTML string
     */
    public String getHtml() {
        return html.toString();
    }

    /**
     * Handles empty lines by closing open list items and paragraphs.
     */
    public void handleEmptyLine() {
        if (inListItem) {
            html.append("</li>\n");
            inListItem = false;
        }
        if (inParagraph) {
            html.append("</p>\n");
            inParagraph = false;
        }
    }

    /**
     * Closes all open HTML elements.
     * Called at the end of HTML generation to ensure valid markup.
     */
    public void closeAll() {
        if (inListItem) {
            html.append("</li>\n");
        }
        if (inOlAlpha) {
            html.append("      </ol>\n");
        }
        if (inParagraph) {
            html.append("</p>\n");
        }
        if (inOl) {
            html.append("    </ol>\n");
        }
        if (inUl) {
            html.append("    </ul>\n");
        }
    }

    /**
     * Checks if currently inside a paragraph element.
     *
     * @return true if in a paragraph
     */
    public boolean isInParagraph() {
        return inParagraph;
    }

    /**
     * Sets the paragraph state.
     *
     * @param inParagraph true if entering a paragraph
     */
    public void setInParagraph(boolean inParagraph) {
        this.inParagraph = inParagraph;
    }

    /**
     * Checks if currently inside a numeric ordered list.
     *
     * @return true if in an ordered list
     */
    public boolean isInOl() {
        return inOl;
    }

    /**
     * Sets the numeric ordered list state.
     *
     * @param inOl true if entering an ordered list
     */
    public void setInOl(boolean inOl) {
        this.inOl = inOl;
    }

    /**
     * Checks if currently inside an unordered list.
     *
     * @return true if in an unordered list
     */
    public boolean isInUl() {
        return inUl;
    }

    /**
     * Sets the unordered list state.
     *
     * @param inUl true if entering an unordered list
     */
    public void setInUl(boolean inUl) {
        this.inUl = inUl;
    }

    /**
     * Checks if currently inside an alphabetical ordered list.
     *
     * @return true if in an alphabetical ordered list
     */
    public boolean isInOlAlpha() {
        return inOlAlpha;
    }

    /**
     * Sets the alphabetical ordered list state.
     *
     * @param inOlAlpha true if entering an alphabetical ordered list
     */
    public void setInOlAlpha(boolean inOlAlpha) {
        this.inOlAlpha = inOlAlpha;
    }

    /**
     * Checks if currently inside a list item.
     *
     * @return true if in a list item
     */
    public boolean isInListItem() {
        return inListItem;
    }

    /**
     * Sets the list item state.
     *
     * @param inListItem true if entering a list item
     */
    public void setInListItem(boolean inListItem) {
        this.inListItem = inListItem;
    }
}
