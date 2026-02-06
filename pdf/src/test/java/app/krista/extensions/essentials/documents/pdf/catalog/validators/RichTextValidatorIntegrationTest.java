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

package app.krista.extensions.essentials.documents.pdf.catalog.validators;

import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.FieldTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for RichTextValidator.
 * Tests all validation scenarios, HTML tag stripping, edge cases, and metadata methods.
 */
@DisplayName("RichTextValidator Integration Tests")
public class RichTextValidatorIntegrationTest {

    private RichTextValidator validator;
    private Map<Validator.ValidationResource, String> context;
    private static final int MIN_CONTENT_LENGTH = 5;

    @BeforeEach
    void setUp() {
        validator = new RichTextValidator();
        context = new HashMap<>();
    }

    // ==================== Valid Rich Text Content Tests ====================

    @Test
    @DisplayName("Should validate plain text with minimum length")
    void testValidate_PlainTextMinimumLength() {
        // Arrange
        String content = "Hello"; // Exactly 5 characters

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept plain text with exactly 5 characters");
    }

    @Test
    @DisplayName("Should validate plain text longer than minimum")
    void testValidate_PlainTextLongerThanMinimum() {
        // Arrange
        String content = "Hello World";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept plain text longer than minimum");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Hello World",
            "This is valid content",
            "12345",
            "ABCDE",
            "a b c d e",
            "Valid content with more than minimum characters",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit"
    })
    @DisplayName("Should validate various valid plain text contents")
    void testValidate_VariousValidPlainText(String content) {
        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept valid plain text: " + content);
    }

    // ==================== Valid HTML Content Tests ====================

    @Test
    @DisplayName("Should validate simple HTML with sufficient content")
    void testValidate_SimpleHtml() {
        // Arrange
        String content = "<p>Hello World</p>";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept HTML with sufficient content after tag stripping");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<p>Hello</p>",
            "<div>Hello World</div>",
            "<span>Valid content</span>",
            "<h1>Title</h1>",
            "<strong>Bold text here</strong>",
            "<em>Italic text</em>",
            "<b>Bold text</b>",
            "<i>Italic text</i>",
            "<u>Underline text</u>"
    })
    @DisplayName("Should validate HTML with various tags")
    void testValidate_VariousHtmlTags(String content) {
        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept HTML content: " + content);
    }

    @Test
    @DisplayName("Should validate complex nested HTML")
    void testValidate_ComplexNestedHtml() {
        // Arrange
        String content = "<div><p>Hello <strong>World</strong></p></div>";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept complex nested HTML with sufficient content");
    }

    @Test
    @DisplayName("Should validate HTML with attributes")
    void testValidate_HtmlWithAttributes() {
        // Arrange
        String content = "<p class='text' id='para1'>Hello World</p>";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept HTML with attributes");
    }

    @Test
    @DisplayName("Should validate HTML with multiple elements")
    void testValidate_MultipleHtmlElements() {
        // Arrange
        String content = "<p>First paragraph</p><p>Second paragraph</p>";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept HTML with multiple elements");
    }

    @Test
    @DisplayName("Should strip HTML tags and validate content length")
    void testValidate_HtmlTagStripping() {
        // Arrange - HTML with exactly 5 characters of content
        String content = "<p><strong><em>Hello</em></strong></p>";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should strip all HTML tags and validate content length");
    }

    @Test
    @DisplayName("Should validate rich text with line breaks")
    void testValidate_RichTextWithLineBreaks() {
        // Arrange
        String content = "<p>Line 1<br/>Line 2</p>";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept rich text with line breaks");
    }

    // ==================== Invalid Content Tests ====================

    @Test
    @DisplayName("Should reject null content")
    void testValidate_NullContent() {
        // Act
        Boolean result = validator.validate(null, context);

        // Assert
        assertFalse(result, "Should reject null content");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n", "  \t\n  "})
    @DisplayName("Should reject null, empty, or blank content")
    void testValidate_NullEmptyBlank(String content) {
        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertFalse(result, "Should reject null/empty/blank content");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "a",
            "ab",
            "abc",
            "abcd"
    })
    @DisplayName("Should reject content shorter than minimum length")
    void testValidate_TooShort(String content) {
        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertFalse(result, "Should reject content shorter than 5 characters: " + content);
    }

    @Test
    @DisplayName("Should reject HTML with insufficient content after tag stripping")
    void testValidate_HtmlInsufficientContent() {
        // Arrange - HTML with only 4 characters of content
        String content = "<p>abcd</p>";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertFalse(result, "Should reject HTML with less than 5 characters of content");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<p></p>",
            "<div></div>",
            "<span></span>",
            "<p>    </p>",
            "<div>   </div>",
            "<p>\t\n</p>"
    })
    @DisplayName("Should reject empty HTML tags")
    void testValidate_EmptyHtmlTags(String content) {
        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertFalse(result, "Should reject empty HTML tags: " + content);
    }

    @Test
    @DisplayName("Should reject HTML with only tags and no content")
    void testValidate_OnlyHtmlTags() {
        // Arrange
        String content = "<p><strong><em></em></strong></p>";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertFalse(result, "Should reject HTML with only tags and no content");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<p>a</p>",
            "<p>ab</p>",
            "<p>abc</p>",
            "<div>abcd</div>",
            "<span>test</span>"
    })
    @DisplayName("Should reject HTML with content shorter than minimum after tag stripping")
    void testValidate_HtmlTooShortAfterStripping(String content) {
        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertFalse(result, "Should reject HTML with insufficient content after stripping: " + content);
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle content with only whitespace")
    void testValidate_OnlyWhitespace() {
        // Arrange
        String content = "     ";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertFalse(result, "Should reject content with only whitespace");
    }

    @Test
    @DisplayName("Should handle HTML with whitespace padding")
    void testValidate_HtmlWithWhitespacePadding() {
        // Arrange
        String content = "<p>  Hello World  </p>";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept HTML with whitespace padding if content is sufficient");
    }

    @Test
    @DisplayName("Should handle very long content")
    void testValidate_VeryLongContent() {
        // Arrange
        String content = "a".repeat(10000);

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept very long content");
    }

    @Test
    @DisplayName("Should handle content with special characters")
    void testValidate_SpecialCharacters() {
        // Arrange
        String[] contents = {
                "Hello!",
                "Test@123",
                "Price: $100",
                "50% off",
                "Email: test@example.com",
                "Path: /home/user",
                "Math: 2+2=4"
        };

        // Act & Assert
        for (String content : contents) {
            Boolean result = validator.validate(content, context);
            assertTrue(result, "Should accept content with special characters: " + content);
        }
    }

    @Test
    @DisplayName("Should handle content with Unicode characters")
    void testValidate_UnicodeCharacters() {
        // Arrange - Ensure all have at least 5 characters
        String[] contents = {
                "你好世界朋友",  // 6 Chinese characters
                "Привет мир",    // 10 characters (Cyrillic)
                "مرحبا بالعالم",  // Arabic text
                "नमस्ते दुनिया",  // Hindi text
                "Hello 世界!"     // Mixed content
        };

        // Act & Assert
        for (String content : contents) {
            Boolean result = validator.validate(content, context);
            assertTrue(result, "Should accept content with Unicode characters: " + content);
        }
    }

    @Test
    @DisplayName("Should handle HTML with malformed tags")
    void testValidate_MalformedHtmlTags() {
        // Arrange - Content with unclosed tags, but sufficient text content
        String content = "<p>Hello World";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept content with malformed tags if text content is sufficient");
    }

    @Test
    @DisplayName("Should handle content with numbers only")
    void testValidate_NumbersOnly() {
        // Arrange
        String content = "12345";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept content with numbers only if length is sufficient");
    }

    @Test
    @DisplayName("Should handle mixed content with HTML and plain text")
    void testValidate_MixedContent() {
        // Arrange
        String content = "Plain text <p>HTML text</p> more plain text";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept mixed HTML and plain text content");
    }

    @Test
    @DisplayName("Should handle self-closing HTML tags")
    void testValidate_SelfClosingTags() {
        // Arrange
        String content = "<p>Hello<br/>World<hr/></p>";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept content with self-closing tags");
    }

    @Test
    @DisplayName("Should handle HTML entities")
    void testValidate_HtmlEntities() {
        // Arrange
        String content = "<p>Hello&nbsp;World&amp;More</p>";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept content with HTML entities");
    }

    // ==================== Metadata Method Tests ====================

    @Test
    @DisplayName("Should return correct field name")
    void testGetFetchFieldName() {
        // Act
        String fieldName = validator.getFetchFieldName();

        // Assert
        assertEquals("Rich Text Data", fieldName, "Should return 'Rich Text Data'");
        assertNotNull(fieldName, "Field name should not be null");
        assertFalse(fieldName.isBlank(), "Field name should not be blank");
    }

    @Test
    @DisplayName("Should return correct field type")
    void testGetFieldType() {
        // Act
        String fieldType = validator.getFieldType();

        // Assert
        assertEquals(FieldTypes.RICH_TEXT_FIELD, fieldType, "Should return RICH_TEXT_FIELD type");
        assertNotNull(fieldType, "Field type should not be null");
        assertFalse(fieldType.isBlank(), "Field type should not be blank");
    }

    @Test
    @DisplayName("Should return meaningful fetch step message")
    void testGetFetchStepMessage() {
        // Act
        String message = validator.getFetchStepMessage();

        // Assert
        assertNotNull(message, "Fetch step message should not be null");
        assertFalse(message.isBlank(), "Fetch step message should not be blank");
        assertTrue(message.contains(String.valueOf(MIN_CONTENT_LENGTH)),
                "Message should mention minimum content length");
        assertEquals("Please provide rich text content with at least 5 characters.", message,
                "Should return expected fetch step message");
    }

    @Test
    @DisplayName("Should return confirmation step message")
    void testGetConfirmationStepMessage() {
        // Arrange
        String content = "test";

        // Act
        String message = validator.getConfirmationStepMessage(content, context);

        // Assert
        assertNotNull(message, "Confirmation message should not be null");
        assertFalse(message.isBlank(), "Confirmation message should not be blank");
        assertTrue(message.contains(String.valueOf(MIN_CONTENT_LENGTH)),
                "Message should mention minimum content length");
        assertEquals("The rich text content is empty or too short. Please provide content with at least 5 characters.",
                message, "Should return expected confirmation message");
    }

    @Test
    @DisplayName("Should return confirmation message with null content")
    void testGetConfirmationStepMessage_NullContent() {
        // Act
        String message = validator.getConfirmationStepMessage(null, context);

        // Assert
        assertNotNull(message, "Confirmation message should not be null");
        assertTrue(message.contains(String.valueOf(MIN_CONTENT_LENGTH)),
                "Message should mention minimum content length");
    }

    @Test
    @DisplayName("Should return confirmation message with null context")
    void testGetConfirmationStepMessage_NullContext() {
        // Arrange
        String content = "test";

        // Act
        String message = validator.getConfirmationStepMessage(content, null);

        // Assert
        assertNotNull(message, "Confirmation message should not be null");
        assertTrue(message.contains(String.valueOf(MIN_CONTENT_LENGTH)),
                "Message should mention minimum content length");
    }

    @Test
    @DisplayName("Should return error message for invalid content")
    void testGetErrMessage() {
        // Arrange
        String content = "test";

        // Act
        String message = validator.getErrMessage(content);

        // Assert
        assertNotNull(message, "Error message should not be null");
        assertFalse(message.isBlank(), "Error message should not be blank");
        assertTrue(message.contains(String.valueOf(MIN_CONTENT_LENGTH)),
                "Message should mention minimum content length");
        assertEquals("Rich text content cannot be empty or blank. Please provide meaningful content with at least 5 characters.",
                message, "Should return expected error message");
    }

    @Test
    @DisplayName("Should return error message for null content")
    void testGetErrMessage_NullContent() {
        // Act
        String message = validator.getErrMessage(null);

        // Assert
        assertNotNull(message, "Error message should not be null");
        assertTrue(message.contains(String.valueOf(MIN_CONTENT_LENGTH)),
                "Message should mention minimum content length");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("Should return error message for blank content")
    void testGetErrMessage_BlankContent(String content) {
        // Act
        String message = validator.getErrMessage(content);

        // Assert
        assertNotNull(message, "Error message should not be null");
        assertTrue(message.contains(String.valueOf(MIN_CONTENT_LENGTH)),
                "Message should mention minimum content length");
    }

    // ==================== Context Handling Tests ====================

    @Test
    @DisplayName("Should validate with empty context map")
    void testValidate_EmptyContext() {
        // Arrange
        String content = "Hello World";
        Map<Validator.ValidationResource, String> emptyContext = new HashMap<>();

        // Act
        Boolean result = validator.validate(content, emptyContext);

        // Assert
        assertTrue(result, "Should validate successfully with empty context");
    }

    @Test
    @DisplayName("Should validate with null context")
    void testValidate_NullContext() {
        // Arrange
        String content = "Hello World";

        // Act
        Boolean result = validator.validate(content, null);

        // Assert
        assertTrue(result, "Should validate successfully with null context");
    }

    @Test
    @DisplayName("Should validate with populated context")
    void testValidate_PopulatedContext() {
        // Arrange
        String content = "Hello World";
        context.put(Validator.ValidationResource.RICH_TEXT_CONTENT, "Some content");
        context.put(Validator.ValidationResource.PDF_TO_HTML_FILE_FORMAT, "file.pdf");

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should validate successfully with populated context");
    }

    // ==================== Boundary Tests ====================

    @Test
    @DisplayName("Should accept content with exactly minimum length")
    void testValidate_ExactlyMinimumLength() {
        // Arrange
        String content = "12345"; // Exactly 5 characters

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept content with exactly 5 characters");
    }

    @Test
    @DisplayName("Should reject content with one less than minimum length")
    void testValidate_OneLessThanMinimum() {
        // Arrange
        String content = "1234"; // 4 characters

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertFalse(result, "Should reject content with 4 characters");
    }

    @Test
    @DisplayName("Should accept content with one more than minimum length")
    void testValidate_OneMoreThanMinimum() {
        // Arrange
        String content = "123456"; // 6 characters

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept content with 6 characters");
    }

    @Test
    @DisplayName("Should handle HTML with exactly minimum content after stripping")
    void testValidate_HtmlExactlyMinimumAfterStripping() {
        // Arrange
        String content = "<p><strong>12345</strong></p>"; // Exactly 5 characters after stripping

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept HTML with exactly 5 characters after tag stripping");
    }

    @Test
    @DisplayName("Should reject HTML with one less than minimum after stripping")
    void testValidate_HtmlOneLessThanMinimumAfterStripping() {
        // Arrange
        String content = "<p><strong>1234</strong></p>"; // 4 characters after stripping

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertFalse(result, "Should reject HTML with 4 characters after tag stripping");
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Should validate and return consistent results")
    void testValidate_ConsistentResults() {
        // Arrange
        String validContent = "Hello World";
        String invalidContent = "Hi";

        // Act
        Boolean result1 = validator.validate(validContent, context);
        Boolean result2 = validator.validate(validContent, context);
        Boolean result3 = validator.validate(invalidContent, context);
        Boolean result4 = validator.validate(invalidContent, context);

        // Assert
        assertTrue(result1, "First validation of valid content should pass");
        assertTrue(result2, "Second validation of valid content should pass");
        assertFalse(result3, "First validation of invalid content should fail");
        assertFalse(result4, "Second validation of invalid content should fail");
        assertEquals(result1, result2, "Multiple validations of same valid content should return same result");
        assertEquals(result3, result4, "Multiple validations of same invalid content should return same result");
    }

    @Test
    @DisplayName("Should work correctly with all interface methods")
    void testAllInterfaceMethods() {
        // Arrange
        String validContent = "Hello World";
        String invalidContent = "Hi";

        // Act & Assert - Test all methods work together
        assertEquals("Rich Text Data", validator.getFetchFieldName());
        assertEquals(FieldTypes.RICH_TEXT_FIELD, validator.getFieldType());
        assertNotNull(validator.getFetchStepMessage());

        assertTrue(validator.validate(validContent, context));
        assertFalse(validator.validate(invalidContent, context));

        assertNotNull(validator.getConfirmationStepMessage(validContent, context));
        assertNotNull(validator.getConfirmationStepMessage(invalidContent, context));
        assertNotNull(validator.getConfirmationStepMessage(null, context));

        assertNotNull(validator.getErrMessage(validContent));
        assertNotNull(validator.getErrMessage(invalidContent));
        assertNotNull(validator.getErrMessage(null));
    }

    @Test
    @DisplayName("Should handle rapid successive validations")
    void testValidate_RapidSuccessiveValidations() {
        // Arrange
        String content = "Hello World";

        // Act & Assert - Validate 100 times rapidly
        for (int i = 0; i < 100; i++) {
            Boolean result = validator.validate(content, context);
            assertTrue(result, "Validation #" + i + " should pass");
        }
    }

    @Test
    @DisplayName("Should handle alternating valid and invalid validations")
    void testValidate_AlternatingValidations() {
        // Arrange
        String validContent = "Hello World";
        String invalidContent = "Hi";

        // Act & Assert - Alternate between valid and invalid
        for (int i = 0; i < 50; i++) {
            Boolean validResult = validator.validate(validContent, context);
            Boolean invalidResult = validator.validate(invalidContent, context);

            assertTrue(validResult, "Valid content should pass on iteration " + i);
            assertFalse(invalidResult, "Invalid content should fail on iteration " + i);
        }
    }

    @Test
    @DisplayName("Should handle content with leading and trailing whitespace in HTML")
    void testValidate_HtmlLeadingTrailingWhitespace() {
        // Arrange
        String content = "<p>   Hello World   </p>";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept HTML with leading/trailing whitespace if content is sufficient");
    }

    @Test
    @DisplayName("Should handle complex real-world rich text")
    void testValidate_ComplexRealWorldRichText() {
        // Arrange
        String content = "<div class='editor'><h1>Title</h1><p>This is a <strong>bold</strong> and <em>italic</em> text.</p><ul><li>Item 1</li><li>Item 2</li></ul></div>";

        // Act
        Boolean result = validator.validate(content, context);

        // Assert
        assertTrue(result, "Should accept complex real-world rich text content");
    }
}




