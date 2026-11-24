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
 * Enumeration of text line types detected during PDF-to-HTML conversion.
 */
public enum LineType {
    /** Heading line (typically all caps or title case). */
    HEADING,

    /** Numeric ordered list item (e.g., "1. Item", "2) Item"). */
    ORDERED_ITEM,

    /** Alphabetical ordered list item (e.g., "a. Item", "b) Item"). */
    ORDERED_ITEM_ALPHA,

    /** Bullet list item (e.g., "• Item", "- Item", "* Item"). */
    BULLET_ITEM,

    /** Regular paragraph text. */
    PARAGRAPH
}
