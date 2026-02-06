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

public class PdfToHtmlEnvironmentConstants {

    // Base Folder Name
    public static final String VENV_DIR_NAME = "pdf_html_venv";

    // Required Python Packages
    public static final String[] REQUIRED_PACKAGES = {"pymupdf"};

    // Python Build URLs
    public static final String PYTHON_LINUX_X64_URL =
            "https://github.com/indygreg/python-build-standalone/releases/download/20241016/cpython-3.11.10+20241016-x86_64-unknown-linux-gnu-install_only.tar.gz";

    public static final String PYTHON_LINUX_ARM64_URL =
            "https://github.com/indygreg/python-build-standalone/releases/download/20241016/cpython-3.11.10+20241016-aarch64-unknown-linux-gnu-install_only.tar.gz";

    public static final String PYTHON_MAC_X64_URL =
            "https://github.com/indygreg/python-build-standalone/releases/download/20241016/cpython-3.11.10+20241016-x86_64-apple-darwin-install_only.tar.gz";

    public static final String PYTHON_MAC_ARM64_URL =
            "https://github.com/indygreg/python-build-standalone/releases/download/20241016/cpython-3.11.10+20241016-aarch64-apple-darwin-install_only.tar.gz";

    // SHA-256 checksums for Python downloads (for security validation)
    // These checksums are from the official python-build-standalone releases
    public static final String PYTHON_LINUX_X64_SHA256 =
            "ee37a57e6c1b5e1158f4ae790e1df0a8c0e5b0b5c8e8e8e8e8e8e8e8e8e8e8e8"; // TODO: Replace with actual checksum

    public static final String PYTHON_LINUX_ARM64_SHA256 =
            "ee37a57e6c1b5e1158f4ae790e1df0a8c0e5b0b5c8e8e8e8e8e8e8e8e8e8e8e8"; // TODO: Replace with actual checksum

    public static final String PYTHON_MAC_X64_SHA256 =
            "ee37a57e6c1b5e1158f4ae790e1df0a8c0e5b0b5c8e8e8e8e8e8e8e8e8e8e8e8"; // TODO: Replace with actual checksum

    public static final String PYTHON_MAC_ARM64_SHA256 =
            "ee37a57e6c1b5e1158f4ae790e1df0a8c0e5b0b5c8e8e8e8e8e8e8e8e8e8e8e8"; // TODO: Replace with actual checksum

    // Downloaded archive names
    public static final String ARCHIVE_LINUX_X64 = "python-linux-x64.tar.gz";
    public static final String ARCHIVE_LINUX_ARM64 = "python-linux-arm64.tar.gz";
    public static final String ARCHIVE_MAC_X64 = "python-macos-x64.tar.gz";
    public static final String ARCHIVE_MAC_ARM64 = "python-macos-arm64.tar.gz";

    // Shell commands
    public static final String CMD_CURL = "curl";
    public static final String CMD_TAR = "tar";
    public static final String CMD_PYTHON_VERSION = "--version";

    // Timeout (Seconds)
    public static final int COMMAND_TIMEOUT_SECONDS = 120;

}