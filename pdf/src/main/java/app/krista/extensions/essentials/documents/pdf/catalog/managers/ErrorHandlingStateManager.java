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

import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.SubCatalogConstants;
import app.krista.extensions.util.KeyValueStore;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Map;

@Service
public class ErrorHandlingStateManager {

    private final KeyValueStore entries;

    @Inject
    public ErrorHandlingStateManager(KeyValueStore internalState) {
        this.entries = internalState;
    }

    public void put(String key, Map<String, Object> value) {
        Object keystoreValue = SubCatalogConstants.GSON.toJson(value);
        entries.put(key, keystoreValue);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String key) {
        final String value = String.valueOf(entries.get(key));
        return SubCatalogConstants.GSON.fromJson(value, Map.class);
    }

    /**
     * Removes a key-value pair from the error handling state.
     *
     * @param key The key to remove
     * @return true if the key was removed, false if it didn't exist
     */
    public boolean remove(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }

        if (entries.get(key) == null) {
            return false;
        }

        entries.remove(key);
        return true;
    }

}
