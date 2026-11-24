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

import java.util.Arrays;
import java.util.List;

/**
 * Processes raw text from PDFs and converts it to structured HTML.
 * Uses pattern matching to identify headings, lists, and paragraphs.
 */
public class HtmlTextProcessor {

    /**
     * Constructs a new HtmlTextProcessor.
     */
    public HtmlTextProcessor() {
        // Default constructor
    }

    /**
     * Converts raw text to HTML by classifying and processing each line.
     *
     * @param rawText the raw text extracted from PDF
     * @return formatted HTML string
     */
    public String buildHtmlFromText(String rawText) {
        HtmlGenerationContext context = new HtmlGenerationContext();
        String[] lines = rawText.split("\\R");

        List<LineHandler> handlers = Arrays.asList(
                new HeadingHandler(),
                new OrderedItemHandler(),
                new OrderedAlphaItemHandler(),
                new BulletItemHandler(),
                new ParagraphHandler());

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                context.handleEmptyLine();
                continue;
            }

            LineType type = classifyLine(trimmed);
            boolean handled = false;
            for (LineHandler handler : handlers) {
                if (handler.canHandle(type)) {
                    handler.handle(trimmed, context);
                    handled = true;
                    break;
                }
            }
            if (!handled) {
                // Fallback to paragraph handling
                new ParagraphHandler().handle(trimmed, context);
            }
        }
        context.closeAll();
        return context.getHtml();
    }

    /**
     * Classifies a line into a specific type based on pattern matching.
     *
     * @param line the line to classify
     * @return the detected line type
     */
    private LineType classifyLine(String line) {
        String trimmed = line.trim();
        // Numbered list: "1. Something", "2) Something"
        if (trimmed.matches("^\\d+([\\.)])\\s+.*")) {
            return LineType.ORDERED_ITEM;
        }
        // Alphabetical list: "a. Something", "b) Something", "A. Something", "B)
        // Something"
        if (trimmed.matches("^[a-zA-Z]([\\.)])\\s+.*")) {
            return LineType.ORDERED_ITEM_ALPHA;
        }
        // Bullet list: "- ", "• ", "* "
        if (trimmed.matches("^[\\-•*]\\s+.*")) {
            return LineType.BULLET_ITEM;
        }
        // Heading heuristic
        if (isHeadingCandidate(trimmed)) {
            return LineType.HEADING;
        }
        return LineType.PARAGRAPH;
    }

    /**
     * Determines if a line is likely a heading based on capitalization and length.
     *
     * @param line the line to check
     * @return true if the line appears to be a heading
     */
    private boolean isHeadingCandidate(String line) {
        if (line.length() > 80) {
            return false;
        }
        // Many PDF headings are ALL CAPS
        boolean allCaps = line.equals(line.toUpperCase());
        // Or "Title Case" (most words start with capital)
        String[] words = line.split("\\s+");
        int capStartCount = 0;
        for (String w : words) {
            if (w.length() > 0 && Character.isUpperCase(w.charAt(0))) {
                capStartCount++;
            }
        }
        boolean mostlyCapStart = words.length > 0 && capStartCount >= (words.length * 0.6);
        // Avoid lines that obviously look like sentences
        boolean endsLikeSentence = line.endsWith(".") || line.endsWith(":");
        return !endsLikeSentence && (allCaps || mostlyCapStart);
    }

    /**
     * Removes list markers from a line (numeric, alphabetic, or bullet).
     *
     * @param line the line with list marker
     * @return the line with marker removed
     */
    public static String removeListMarker(String line) {
        // Strip numbered marker "1. ", "2) "
        String withoutOrdered = line.replaceFirst("^\\d+([\\.)])\\s+", "");
        // Strip alphabetical marker "a. ", "b) ", "A. ", "B) "
        withoutOrdered = withoutOrdered.replaceFirst("^[a-zA-Z]([\\.)])\\s+", "");
        // Strip bullet marker "- ", "• ", "* "
        return withoutOrdered.replaceFirst("^[\\-•*]\\s+", "");
    }

    /**
     * Escapes HTML special characters to prevent XSS attacks.
     *
     * @param s the string to escape
     * @return HTML-safe string
     */
    public static String escapeHtml(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                case '\'':
                    out.append("&#39;");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }
}
