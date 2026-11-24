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

package app.krista.extensions.essentials.documents.pdf.helper;

import app.krista.ksdk.telemetry.TelemetryMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for TelemetryHelper covering success/error recording,
 * tag sanitization, and the safeTagMap helper.
 */
@DisplayName("TelemetryHelper Unit Tests")
public class TelemetryHelperTest {

    @Mock
    private TelemetryMetrics telemetry;

    private TelemetryHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        helper = new TelemetryHelper(telemetry);
    }

    // ==================== recordSuccess ====================

    @Test
    @DisplayName("recordSuccess increments the success counter and records duration with status tag")
    void testRecordSuccess_NullAdditionalTags() {
        long startTime = System.currentTimeMillis() - 50;

        helper.recordSuccess("pdf.mergePdfFiles", startTime, null);

        verify(telemetry).incrementCounter(eq("pdf.mergePdfFiles.success"), eq(1L));

        ArgumentCaptor<Long> durationCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map> tagsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(telemetry).recordDuration(eq("pdf.mergePdfFiles.duration"), durationCaptor.capture(), tagsCaptor.capture());

        assertTrue(durationCaptor.getValue() >= 0, "duration should be non-negative");

        Map<String, String> tags = tagsCaptor.getValue();
        assertEquals(1, tags.size());
        assertEquals("success", tags.get("status"));
    }

    @Test
    @DisplayName("recordSuccess merges additional tags and sanitizes null values")
    void testRecordSuccess_WithAdditionalTagsContainingNull() {
        Map<String, String> additional = new HashMap<>();
        additional.put("fileType", "pdf");
        additional.put("owner", null);

        helper.recordSuccess("pdf.split", System.currentTimeMillis(), additional);

        verify(telemetry).incrementCounter("pdf.split.success", 1);

        ArgumentCaptor<Map> tagsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(telemetry).recordDuration(eq("pdf.split.duration"), anyLong(), tagsCaptor.capture());

        Map<String, String> tags = tagsCaptor.getValue();
        assertEquals("success", tags.get("status"));
        assertEquals("pdf", tags.get("fileType"));
        assertEquals("NA", tags.get("owner"), "null tag values should be sanitized to NA");
    }

    @Test
    @DisplayName("recordSuccess with empty additional tags map results in only the status tag")
    void testRecordSuccess_EmptyAdditionalTags() {
        helper.recordSuccess("pdf.merge", System.currentTimeMillis(), Map.of());

        ArgumentCaptor<Map> tagsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(telemetry).recordDuration(eq("pdf.merge.duration"), anyLong(), tagsCaptor.capture());

        assertEquals(Map.of("status", "success"), tagsCaptor.getValue());
    }

    // ==================== recordError ====================

    @Test
    @DisplayName("recordError increments the error counter and records exception details")
    void testRecordError_WithMessage() {
        Exception ex = new IllegalStateException("boom");

        helper.recordError("pdf.convert", System.currentTimeMillis(), ex, null);

        verify(telemetry).incrementCounter("pdf.convert.error", 1);

        ArgumentCaptor<Map> tagsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(telemetry).recordDuration(eq("pdf.convert.duration"), anyLong(), tagsCaptor.capture());

        Map<String, String> tags = tagsCaptor.getValue();
        assertEquals("error", tags.get("status"));
        assertEquals("general", tags.get("error_type"));
        assertEquals("boom", tags.get("error_message"));
        assertEquals("IllegalStateException", tags.get("error_class"));
    }

    @Test
    @DisplayName("recordError falls back to 'Unknown error' when exception message is null")
    void testRecordError_NullMessage() {
        Exception ex = new RuntimeException();

        helper.recordError("pdf.convert", System.currentTimeMillis(), ex, null);

        ArgumentCaptor<Map> tagsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(telemetry).recordDuration(eq("pdf.convert.duration"), anyLong(), tagsCaptor.capture());

        Map<String, String> tags = tagsCaptor.getValue();
        assertEquals("Unknown error", tags.get("error_message"));
        assertEquals("RuntimeException", tags.get("error_class"));
    }

    @Test
    @DisplayName("recordError merges sanitized additional tags with error details")
    void testRecordError_WithAdditionalTags() {
        Map<String, String> additional = new HashMap<>();
        additional.put("stage", "upload");
        additional.put("extra", null);

        Exception ex = new IllegalArgumentException("bad arg");
        helper.recordError("pdf.upload", System.currentTimeMillis(), ex, additional);

        ArgumentCaptor<Map> tagsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(telemetry).recordDuration(eq("pdf.upload.duration"), anyLong(), tagsCaptor.capture());

        Map<String, String> tags = tagsCaptor.getValue();
        assertEquals("error", tags.get("status"));
        assertEquals("upload", tags.get("stage"));
        assertEquals("NA", tags.get("extra"));
        assertEquals("bad arg", tags.get("error_message"));
        assertEquals("IllegalArgumentException", tags.get("error_class"));
    }

    // ==================== incrementCount ====================

    @Test
    @DisplayName("incrementCount increments the count metric exactly once and nothing else")
    void testIncrementCount() {
        helper.incrementCount("pdf.mergePdfFiles");

        verify(telemetry, times(1)).incrementCounter("pdf.mergePdfFiles.count", 1);
        verifyNoMoreInteractions(telemetry);
    }

    // ==================== safeTagMap ====================

    @Test
    @DisplayName("safeTagMap builds a map from alternating key/value pairs")
    void testSafeTagMap_Basic() {
        Map<String, String> result = TelemetryHelper.safeTagMap("k1", "v1", "k2", "v2");

        assertEquals(2, result.size());
        assertEquals("v1", result.get("k1"));
        assertEquals("v2", result.get("k2"));
    }

    @Test
    @DisplayName("safeTagMap replaces null values with NA")
    void testSafeTagMap_NullValue() {
        Map<String, String> result = TelemetryHelper.safeTagMap("k1", null, "k2", "v2");

        assertEquals("NA", result.get("k1"));
        assertEquals("v2", result.get("k2"));
    }

    @Test
    @DisplayName("safeTagMap with no arguments returns an empty map")
    void testSafeTagMap_NoArgs() {
        Map<String, String> result = TelemetryHelper.safeTagMap();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("safeTagMap ignores a trailing key with no paired value")
    void testSafeTagMap_OddLength() {
        Map<String, String> result = TelemetryHelper.safeTagMap("k1", "v1", "danglingKey");

        assertEquals(1, result.size());
        assertEquals("v1", result.get("k1"));
        assertFalse(result.containsKey("danglingKey"));
    }

    @Test
    @DisplayName("safeTagMap with a single unpaired key returns an empty map")
    void testSafeTagMap_SingleDanglingKey() {
        Map<String, String> result = TelemetryHelper.safeTagMap("onlyKey");

        assertTrue(result.isEmpty());
    }
}
