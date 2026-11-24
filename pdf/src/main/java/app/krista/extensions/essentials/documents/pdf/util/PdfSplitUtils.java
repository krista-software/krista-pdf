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

package app.krista.extensions.essentials.documents.pdf.util;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for splitting PDF documents without file size bloat.
 *
 * <p>Uses a "reload-and-remove" strategy combined with resource stripping:
 * after removing unwanted pages, each remaining page's content stream is parsed
 * to identify which resources it actually uses. A filtered resource dictionary
 * containing only those resources replaces the original (potentially inherited)
 * one, so that unreferenced fonts, images and colour profiles are omitted when
 * the document is saved.</p>
 */
public final class PdfSplitUtils {

    private static final Logger log = LoggerFactory.getLogger(PdfSplitUtils.class);

    /** Content-stream operators whose first COSName operand is a resource name. */
    private static final Set<String> RESOURCE_OPERATORS = Set.of(
            "Tf",   // set font
            "Do",   // paint XObject (image / form)
            "cs",   // set colour space (stroking)
            "CS",   // set colour space (non-stroking)
            "gs",   // set extended graphics state
            "sh",   // paint shading
            "scn",  // set colour (may reference pattern)
            "SCN",  // set colour (may reference pattern)
            "DP",   // marked-content point with properties
            "BDC"   // begin marked-content with properties
    );

    private PdfSplitUtils() {
        // utility class
    }

    /**
     * Creates a new PDDocument containing only the specified pages from the source file.
     *
     * <p>The caller is responsible for closing the returned document after saving it.</p>
     *
     * @param sourceFile   the original PDF file on disk
     * @param pagesToKeep  zero-based page indices to retain (all others are removed)
     * @return a PDDocument with only the requested pages
     * @throws IOException if the file cannot be read
     * @throws IllegalArgumentException if pagesToKeep is null or empty
     */
    public static PDDocument createDocumentWithPages(File sourceFile, Set<Integer> pagesToKeep) throws IOException {
        if (pagesToKeep == null || pagesToKeep.isEmpty()) {
            throw new IllegalArgumentException("pagesToKeep must not be null or empty");
        }

        PDDocument doc = PDDocument.load(sourceFile);
        int totalPages = doc.getNumberOfPages();

        // Remove pages in reverse order to preserve indices
        for (int i = totalPages - 1; i >= 0; i--) {
            if (!pagesToKeep.contains(i)) {
                doc.removePage(i);
            }
        }

        stripUnusedResources(doc);

        log.debug("Created document with {} pages from {} (original had {} pages)",
                doc.getNumberOfPages(), sourceFile.getName(), totalPages);
        return doc;
    }

    /**
     * Creates a new PDDocument containing a contiguous range of pages from the source file.
     *
     * <p>The caller is responsible for closing the returned document after saving it.</p>
     *
     * @param sourceFile the original PDF file on disk
     * @param startIndex zero-based start index (inclusive)
     * @param endIndex   zero-based end index (exclusive)
     * @return a PDDocument with only the pages in [startIndex, endIndex)
     * @throws IOException if the file cannot be read
     * @throws IllegalArgumentException if the range is invalid
     */
    public static PDDocument createDocumentWithPageRange(File sourceFile, int startIndex, int endIndex) throws IOException {
        if (startIndex < 0 || endIndex <= startIndex) {
            throw new IllegalArgumentException(
                    String.format("Invalid page range: startIndex=%d, endIndex=%d", startIndex, endIndex));
        }

        PDDocument doc = PDDocument.load(sourceFile);
        int totalPages = doc.getNumberOfPages();

        if (endIndex > totalPages) {
            doc.close();
            throw new IllegalArgumentException(
                    String.format("endIndex (%d) exceeds total pages (%d)", endIndex, totalPages));
        }

        // Remove pages after the range first (higher indices), then before (lower indices)
        for (int i = totalPages - 1; i >= endIndex; i--) {
            doc.removePage(i);
        }
        for (int i = startIndex - 1; i >= 0; i--) {
            doc.removePage(i);
        }

        stripUnusedResources(doc);

        log.debug("Created document with pages [{}, {}) from {} (original had {} pages)",
                startIndex, endIndex, sourceFile.getName(), totalPages);
        return doc;
    }

    // ======================== Resource stripping ========================

    /**
     * Filters each page's resource dictionary to keep only resources referenced
     * in its content stream, and removes inherited resources from the page tree
     * root. Unreferenced objects become unreachable and are omitted on save.
     *
     * <p>This method is safe for both split and merge operations — it only
     * touches per-page resources and does <em>not</em> remove document-level
     * catalog entries (bookmarks, metadata, form fields, etc.).</p>
     */
    public static void filterUnusedPageResources(PDDocument doc) throws IOException {
        for (PDPage page : doc.getPages()) {
            PDResources resources = page.getResources();
            if (resources == null) {
                continue;
            }

            Set<COSName> usedNames = collectUsedResourceNames(page);
            PDResources filtered = buildFilteredResources(resources, usedNames);
            page.setResources(filtered);
        }

        // Remove inherited /Resources from the page tree root so the old
        // (unfiltered) dictionary — and everything only reachable through it —
        // becomes unreachable for COSWriter.
        doc.getPages().getCOSObject().removeItem(COSName.RESOURCES);
    }

    /**
     * Filters unused per-page resources <em>and</em> strips document-level
     * catalog overhead (ICC profiles, structure trees, bookmarks, forms, etc.).
     *
     * <p><strong>Use only for split operations</strong> where the output is an
     * individual page extract. Do <em>not</em> use for merge operations — it
     * would destroy legitimate document metadata. Use
     * {@link #filterUnusedPageResources(PDDocument)} instead.</p>
     */
    public static void stripUnusedResources(PDDocument doc) throws IOException {
        filterUnusedPageResources(doc);
        stripDocumentLevelOverhead(doc);
    }

    /**
     * Removes document-level COS entries from the catalog that are irrelevant
     * for individual page splits (ICC profiles, structure trees, forms, etc.).
     * These objects are copied into every split file because they are reachable
     * from the document catalog, even though they serve no purpose for a
     * single-page (or few-page) extract.
     */
    private static void stripDocumentLevelOverhead(PDDocument doc) {
        COSDictionary catalogDict = doc.getDocumentCatalog().getCOSObject();
        catalogDict.removeItem(COSName.OUTPUT_INTENTS);                    // ICC profiles (often 0.5-2 MB)
        catalogDict.removeItem(COSName.STRUCT_TREE_ROOT);                  // tagged-PDF structure tree
        catalogDict.removeItem(COSName.NAMES);                             // embedded files, JS, destinations
        catalogDict.removeItem(COSName.OUTLINES);                          // bookmarks
        catalogDict.removeItem(COSName.ACRO_FORM);                         // form fields
        catalogDict.removeItem(COSName.METADATA);                          // XMP metadata stream
        catalogDict.removeItem(COSName.getPDFName("MarkInfo"));            // mark info dict
        catalogDict.removeItem(COSName.THREADS);                           // article threads
        catalogDict.removeItem(COSName.OPEN_ACTION);                       // viewer open action
    }

    // ======================== Resource deduplication ========================

    /** Resource dictionary keys whose sub-entries are eligible for deduplication. */
    private static final COSName[] DEDUP_CATEGORIES = {
            COSName.XOBJECT, COSName.FONT, COSName.EXT_G_STATE,
            COSName.COLORSPACE, COSName.getPDFName("Shading"),
            COSName.getPDFName("Pattern")
    };

    /** Maximum depth for recursive COS tree hashing (prevents cycles). */
    private static final int MAX_HASH_DEPTH = 12;

    /**
     * Deduplicates content-identical resources across pages. After merging
     * multiple PDF files, each page may carry its own copy of resources
     * (fonts, images, colour spaces, graphics states) that are byte-identical
     * to resources on other pages. This method computes a content hash of
     * each resource's COS object tree, identifies duplicates, and rewires
     * page resource dictionaries so that identical resources share a single
     * COS object. When the document is subsequently saved, each unique
     * resource is written only once.
     */
    public static void deduplicateResources(PDDocument doc) throws IOException {
        Map<String, COSBase> canonical = new HashMap<>();

        for (PDPage page : doc.getPages()) {
            PDResources resources = page.getResources();
            if (resources == null) {
                continue;
            }
            COSDictionary resDict = resources.getCOSObject();

            for (COSName category : DEDUP_CATEGORIES) {
                COSBase catBase = resDict.getDictionaryObject(category);
                if (!(catBase instanceof COSDictionary) || catBase instanceof COSStream) {
                    continue;
                }
                COSDictionary catDict = (COSDictionary) catBase;

                for (COSName name : new ArrayList<>(catDict.keySet())) {
                    COSBase obj = catDict.getDictionaryObject(name);
                    if (obj == null) {
                        continue;
                    }

                    String hash = computeContentHash(category, obj);
                    if (hash == null) {
                        continue;
                    }

                    COSBase existing = canonical.get(hash);
                    if (existing != null && existing != obj) {
                        catDict.setItem(name, existing);
                    } else {
                        canonical.put(hash, obj);
                    }
                }
            }
        }
    }

    /**
     * Computes a SHA-256 content hash of a COS object tree for deduplication.
     * Returns {@code null} if the object cannot be hashed.
     */
    private static String computeContentHash(COSName category, COSBase obj) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(category.getName().getBytes(StandardCharsets.UTF_8));
            digestCOSTree(obj, md, new IdentityHashMap<>(), 0);
            return bytesToHex(md.digest());
        } catch (Exception e) {
            log.debug("Could not hash COS object for deduplication: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Recursively feeds the content of a COS object tree into a
     * {@link MessageDigest}. Handles streams, dictionaries, arrays,
     * and all primitive COS types. Uses an identity map for cycle
     * detection and a depth limit as a safety net.
     */
    private static void digestCOSTree(COSBase obj, MessageDigest md,
                                       IdentityHashMap<COSBase, Boolean> visited,
                                       int depth) throws IOException {
        if (obj == null || depth > MAX_HASH_DEPTH) {
            md.update((byte) 0);
            return;
        }

        // Resolve indirect object references
        if (obj instanceof COSObject) {
            obj = ((COSObject) obj).getObject();
            if (obj == null) {
                md.update((byte) 0);
                return;
            }
        }

        // Cycle detection for compound types
        if (obj instanceof COSDictionary || obj instanceof COSArray) {
            if (visited.put(obj, Boolean.TRUE) != null) {
                md.update((byte) 0xFF);
                return;
            }
        }

        if (obj instanceof COSStream) {
            md.update((byte) 'S');
            COSStream stream = (COSStream) obj;
            try (InputStream is = stream.createRawInputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) {
                    md.update(buf, 0, n);
                }
            }
            digestDictEntries(stream, md, visited, depth);
        } else if (obj instanceof COSDictionary) {
            md.update((byte) 'D');
            digestDictEntries((COSDictionary) obj, md, visited, depth);
        } else if (obj instanceof COSArray) {
            md.update((byte) 'A');
            COSArray arr = (COSArray) obj;
            for (int i = 0; i < arr.size(); i++) {
                digestCOSTree(arr.get(i), md, visited, depth + 1);
            }
        } else if (obj instanceof COSName) {
            md.update((byte) 'N');
            md.update(((COSName) obj).getName().getBytes(StandardCharsets.UTF_8));
        } else if (obj instanceof COSString) {
            md.update((byte) 'T');
            md.update(((COSString) obj).getBytes());
        } else if (obj instanceof COSNumber) {
            md.update((byte) '#');
            md.update(String.valueOf(((COSNumber) obj).floatValue()).getBytes(StandardCharsets.UTF_8));
        } else if (obj instanceof COSBoolean) {
            md.update(((COSBoolean) obj).getValue() ? (byte) 1 : (byte) 2);
        } else {
            md.update(obj.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Feeds dictionary entries into the digest in deterministic (sorted) order.
     * Skips {@code /Length} as it varies with compression and is auto-computed
     * on save.
     */
    private static void digestDictEntries(COSDictionary dict, MessageDigest md,
                                           IdentityHashMap<COSBase, Boolean> visited,
                                           int depth) throws IOException {
        List<COSName> keys = new ArrayList<>(dict.keySet());
        keys.sort(Comparator.comparing(COSName::getName));
        for (COSName key : keys) {
            if (COSName.LENGTH.equals(key)) {
                continue;
            }
            md.update(key.getName().getBytes(StandardCharsets.UTF_8));
            digestCOSTree(dict.getDictionaryObject(key), md, visited, depth + 1);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    // ======================== Content-stream analysis ========================

    /**
     * Parses a page's content stream and collects every {@link COSName} that
     * appears as an operand to a resource-referencing operator.
     */
    private static Set<COSName> collectUsedResourceNames(PDPage page) throws IOException {
        Set<COSName> usedNames = new HashSet<>();

        InputStream contentStream = page.getContents();
        if (contentStream == null) {
            return usedNames;
        }

        try {
            PDFStreamParser parser = new PDFStreamParser(page);
            List<Object> operandWindow = new ArrayList<>(4);
            Object token;
            while ((token = parser.parseNextToken()) != null) {
                if (token instanceof Operator) {
                    String opName = ((Operator) token).getName();
                    if (RESOURCE_OPERATORS.contains(opName)) {
                        for (Object operand : operandWindow) {
                            if (operand instanceof COSName) {
                                usedNames.add((COSName) operand);
                            }
                        }
                    }
                    operandWindow.clear();
                } else {
                    if (operandWindow.size() >= 4) operandWindow.remove(0);
                    operandWindow.add(token);
                }
            }
        } catch (IOException e) {
            // If we can't parse the content stream, keep all resources to be safe
            log.warn("Could not parse content stream for resource filtering; keeping all resources", e);
            addAllResourceNames(page.getResources(), usedNames);
        }

        return usedNames;
    }

    /**
     * Builds a new {@link PDResources} containing only the entries whose names
     * appear in {@code usedNames}.
     */
    private static PDResources buildFilteredResources(PDResources source, Set<COSName> usedNames) throws IOException {
        PDResources filtered = new PDResources();

        // Fonts
        for (COSName name : source.getFontNames()) {
            if (usedNames.contains(name)) {
                PDFont font = source.getFont(name);
                if (font != null) {
                    filtered.put(name, font);
                }
            }
        }

        // XObjects (images, forms)
        for (COSName name : source.getXObjectNames()) {
            if (usedNames.contains(name)) {
                PDXObject xobj = source.getXObject(name);
                if (xobj != null) {
                    filtered.put(name, xobj);
                }
            }
        }

        // Extended graphics states
        for (COSName name : source.getExtGStateNames()) {
            if (usedNames.contains(name)) {
                PDExtendedGraphicsState gs = source.getExtGState(name);
                if (gs != null) {
                    filtered.put(name, gs);
                }
            }
        }

        // Colour spaces
        for (COSName name : source.getColorSpaceNames()) {
            if (usedNames.contains(name)) {
                PDColorSpace cs = source.getColorSpace(name);
                if (cs != null) {
                    filtered.put(name, cs);
                }
            }
        }

        // Shadings
        for (COSName name : source.getShadingNames()) {
            if (usedNames.contains(name)) {
                PDShading shading = source.getShading(name);
                if (shading != null) {
                    filtered.put(name, shading);
                }
            }
        }

        // Patterns
        for (COSName name : source.getPatternNames()) {
            if (usedNames.contains(name)) {
                PDAbstractPattern pattern = source.getPattern(name);
                if (pattern != null) {
                    filtered.put(name, pattern);
                }
            }
        }

        // Copy /ProcSet if present (legacy but still common)
        COSDictionary sourceDict = source.getCOSObject();
        COSBase procSet = sourceDict.getDictionaryObject(COSName.PROC_SET);
        if (procSet != null) {
            filtered.getCOSObject().setItem(COSName.PROC_SET, procSet);
        }

        return filtered;
    }

    /**
     * Adds all resource names from every category into the given set.
     * Used as a fallback when content-stream parsing fails.
     */
    private static void addAllResourceNames(PDResources resources, Set<COSName> names) {
        if (resources == null) return;
        resources.getFontNames().forEach(names::add);
        resources.getXObjectNames().forEach(names::add);
        resources.getExtGStateNames().forEach(names::add);
        resources.getColorSpaceNames().forEach(names::add);
        resources.getShadingNames().forEach(names::add);
        resources.getPatternNames().forEach(names::add);
    }
}
