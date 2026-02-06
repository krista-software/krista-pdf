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

package app.krista.extensions.essentials.documents.pdf.catalog.managers;

import app.krista.extensions.util.KeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ErrorHandlingStateManagerTest {

    @Mock
    private KeyValueStore keyValueStore;

    private ErrorHandlingStateManager stateManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stateManager = new ErrorHandlingStateManager(keyValueStore);
    }

    @Test
    @DisplayName("Should put value into key value store")
    void testPut() {
        Map<String, Object> value = new HashMap<>();
        value.put("field1", "value1");
        value.put("field2", 123);

        stateManager.put("testKey", value);

        verify(keyValueStore).put(eq("testKey"), any(String.class));
    }

    @Test
    @DisplayName("Should get value from key value store")
    void testGet() {
        String jsonValue = "{\"field1\":\"value1\",\"field2\":123}";
        when(keyValueStore.get("testKey")).thenReturn(jsonValue);

        Map<String, Object> result = stateManager.get("testKey");

        assertNotNull(result);
        verify(keyValueStore).get("testKey");
    }

    @Test
    @DisplayName("Should remove existing key")
    void testRemove_ExistingKey() {
        when(keyValueStore.get("testKey")).thenReturn("someValue");

        boolean result = stateManager.remove("testKey");

        assertTrue(result);
        verify(keyValueStore).get("testKey");
        verify(keyValueStore).remove("testKey");
    }

    @Test
    @DisplayName("Should return false when removing non-existing key")
    void testRemove_NonExistingKey() {
        when(keyValueStore.get("nonExistingKey")).thenReturn(null);

        boolean result = stateManager.remove("nonExistingKey");

        assertFalse(result);
        verify(keyValueStore).get("nonExistingKey");
        verify(keyValueStore, never()).remove("nonExistingKey");
    }

    @Test
    @DisplayName("Should return false when removing null key")
    void testRemove_NullKey() {
        boolean result = stateManager.remove(null);

        assertFalse(result);
        verify(keyValueStore, never()).get(any());
        verify(keyValueStore, never()).remove(any());
    }

    @Test
    @DisplayName("Should return false when removing blank key")
    void testRemove_BlankKey() {
        boolean result = stateManager.remove("   ");

        assertFalse(result);
        verify(keyValueStore, never()).get(any());
        verify(keyValueStore, never()).remove(any());
    }

    @Test
    @DisplayName("Should return false when removing empty key")
    void testRemove_EmptyKey() {
        boolean result = stateManager.remove("");

        assertFalse(result);
        verify(keyValueStore, never()).get(any());
        verify(keyValueStore, never()).remove(any());
    }
}

