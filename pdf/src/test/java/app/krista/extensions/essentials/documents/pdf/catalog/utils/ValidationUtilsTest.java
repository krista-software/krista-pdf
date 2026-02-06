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

package app.krista.extensions.essentials.documents.pdf.catalog.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationUtilsTest {

    @Test
    @DisplayName("Should return true for null string")
    void testIsStringNullOrBlank_Null() {
        assertTrue(ValidationUtils.isStringNullOrBlank(null));
    }

    @Test
    @DisplayName("Should return true for blank string")
    void testIsStringNullOrBlank_Blank() {
        assertTrue(ValidationUtils.isStringNullOrBlank("   "));
    }

    @Test
    @DisplayName("Should return true for empty string")
    void testIsStringNullOrBlank_Empty() {
        assertTrue(ValidationUtils.isStringNullOrBlank(""));
    }

    @Test
    @DisplayName("Should return false for non-blank string")
    void testIsStringNullOrBlank_NonBlank() {
        assertFalse(ValidationUtils.isStringNullOrBlank("test"));
    }

    @Test
    @DisplayName("Should return true for null list")
    void testIsListNullOrEmpty_Null() {
        assertTrue(ValidationUtils.isListNullOrEmpty(null));
    }

    @Test
    @DisplayName("Should return true for empty list")
    void testIsListNullOrEmpty_Empty() {
        assertTrue(ValidationUtils.isListNullOrEmpty(new ArrayList<>()));
    }

    @Test
    @DisplayName("Should return false for non-empty list")
    void testIsListNullOrEmpty_NonEmpty() {
        assertFalse(ValidationUtils.isListNullOrEmpty(Arrays.asList("item1", "item2")));
    }

    @Test
    @DisplayName("Should return true for null map")
    void testIsMapNullOrEmpty_Null() {
        assertTrue(ValidationUtils.isMapNullOrEmpty(null));
    }

    @Test
    @DisplayName("Should return true for empty map")
    void testIsMapNullOrEmpty_Empty() {
        assertTrue(ValidationUtils.isMapNullOrEmpty(new HashMap<>()));
    }

    @Test
    @DisplayName("Should return false for non-empty map")
    void testIsMapNullOrEmpty_NonEmpty() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertFalse(ValidationUtils.isMapNullOrEmpty(map));
    }

    @Test
    @DisplayName("Should validate correct email address")
    void testIsEmailValid_ValidEmail() {
        assertTrue(ValidationUtils.isEmailValid("test@example.com"));
        assertTrue(ValidationUtils.isEmailValid("user.name@domain.co.uk"));
        assertTrue(ValidationUtils.isEmailValid("first+last@company.org"));
    }

    @Test
    @DisplayName("Should reject invalid email address")
    void testIsEmailValid_InvalidEmail() {
        assertFalse(ValidationUtils.isEmailValid("invalid"));
        assertFalse(ValidationUtils.isEmailValid("@example.com"));
        assertFalse(ValidationUtils.isEmailValid("test@"));
        assertFalse(ValidationUtils.isEmailValid("test@.com"));
    }

    @Test
    @DisplayName("Should reject null email")
    void testIsEmailValid_Null() {
        assertFalse(ValidationUtils.isEmailValid(null));
    }

    @Test
    @DisplayName("Should reject blank email")
    void testIsEmailValid_Blank() {
        assertFalse(ValidationUtils.isEmailValid("   "));
    }

    @Test
    @DisplayName("Should validate integer number")
    void testIsNumberValid_Integer() {
        assertTrue(ValidationUtils.isNumberValid("123"));
    }

    @Test
    @DisplayName("Should validate decimal number")
    void testIsNumberValid_Decimal() {
        assertTrue(ValidationUtils.isNumberValid("123.45"));
    }

    @Test
    @DisplayName("Should validate negative number")
    void testIsNumberValid_Negative() {
        assertTrue(ValidationUtils.isNumberValid("-123.45"));
    }

    @Test
    @DisplayName("Should reject invalid number")
    void testIsNumberValid_Invalid() {
        assertFalse(ValidationUtils.isNumberValid("abc"));
        assertFalse(ValidationUtils.isNumberValid("12.34.56"));
    }

    @Test
    @DisplayName("Should reject null number")
    void testIsNumberValid_Null() {
        assertFalse(ValidationUtils.isNumberValid(null));
    }

    @Test
    @DisplayName("Should reject blank number")
    void testIsNumberValid_Blank() {
        assertFalse(ValidationUtils.isNumberValid("   "));
    }

    @Test
    @DisplayName("Should validate number in range")
    void testIsNumberInRange_InRange() {
        assertTrue(ValidationUtils.isNumberInRange(5, 1, 10));
        assertTrue(ValidationUtils.isNumberInRange(1, 1, 10));
        assertTrue(ValidationUtils.isNumberInRange(10, 1, 10));
    }

    @Test
    @DisplayName("Should reject number out of range")
    void testIsNumberInRange_OutOfRange() {
        assertFalse(ValidationUtils.isNumberInRange(0, 1, 10));
        assertFalse(ValidationUtils.isNumberInRange(11, 1, 10));
    }

    @Test
    @DisplayName("Should validate string with minimum length")
    void testHasMinLength_Valid() {
        assertTrue(ValidationUtils.hasMinLength("hello", 5));
        assertTrue(ValidationUtils.hasMinLength("hello world", 5));
    }

    @Test
    @DisplayName("Should reject string shorter than minimum length")
    void testHasMinLength_TooShort() {
        assertFalse(ValidationUtils.hasMinLength("hi", 5));
    }

    @Test
    @DisplayName("Should reject null string for minimum length")
    void testHasMinLength_Null() {
        assertFalse(ValidationUtils.hasMinLength(null, 5));
    }

    @Test
    @DisplayName("Should validate string with maximum length")
    void testHasMaxLength_Valid() {
        assertTrue(ValidationUtils.hasMaxLength("hello", 10));
        assertTrue(ValidationUtils.hasMaxLength("hello", 5));
    }

    @Test
    @DisplayName("Should reject string longer than maximum length")
    void testHasMaxLength_TooLong() {
        assertFalse(ValidationUtils.hasMaxLength("hello world", 5));
    }

    @Test
    @DisplayName("Should accept null string for maximum length")
    void testHasMaxLength_Null() {
        assertTrue(ValidationUtils.hasMaxLength(null, 5));
    }
}

