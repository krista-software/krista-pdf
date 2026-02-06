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

package app.krista.extensions.essentials.documents.pdf;

import app.krista.extension.impl.anno.*;
import app.krista.extensions.essentials.documents.pdf.util.PdfToHtmlEnvironmentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Java(version = Java.Version.JAVA_21)
@Extension(version = "2.0.20", name = "PDF", description = "Converts documents to PDF in Krista. v2.1.0: Split PDF output changed to named variables (Pages Before/After Split Page)")
@StaticResource(path = "docs", file = "docs")
public final class PDFExtension {

    private static final Logger logger = LoggerFactory.getLogger(PDFExtension.class);

    /**
     * Provides custom tabs for the extension UI.
     * This method configures the documentation tab that appears in the Krista platform
     * when users interact with this extension.
     *
     * @return A map containing tab names and their corresponding static resource paths
     */
    @InvokerRequest(InvokerRequest.Type.CUSTOM_TABS)
    public Map<String, String> getCustomTabs() {
        return Map.of("Documentation", "static/docs");
    }

    @InvokerRequest(InvokerRequest.Type.INVOKER_LOADED)
    public void invokerLoaded() {
        PdfToHtmlEnvironmentManager.setupEnvironment();
    }

    @InvokerRequest(InvokerRequest.Type.INVOKER_REMOVED)
    public void invokerUnloaded() {
        PdfToHtmlEnvironmentManager.cleanupScriptEnvironment();
    }

}
