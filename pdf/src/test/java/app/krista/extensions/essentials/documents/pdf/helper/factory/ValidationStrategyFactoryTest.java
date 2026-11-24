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

package app.krista.extensions.essentials.documents.pdf.helper.factory;

import app.krista.extensions.essentials.documents.pdf.catalog.generartors.ExtensionResponseGenerator;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.documents.pdf.helper.strategy.ValidationStrategy;
import app.krista.extensions.essentials.documents.pdf.helper.strategy.WithRetryValidationStrategy;
import app.krista.extensions.essentials.documents.pdf.helper.strategy.WithoutRetryValidationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Unit tests for {@link ValidationStrategyFactory#createStrategy(Boolean)}.
 *
 * <p>The method has a single decision point: {@code Boolean.TRUE.equals(allowRetry)}.
 * These tests drive both outcomes, plus the null-input case (which also takes the
 * "false" path since {@code Boolean.TRUE.equals(null)} is false), and verify a fresh
 * strategy instance is created on each call.</p>
 */
@DisplayName("ValidationStrategyFactory Tests")
class ValidationStrategyFactoryTest {

    @Mock
    private ValidationOrchestrator validationOrchestrator;

    @Mock
    private ExtensionResponseGenerator responseGenerator;

    private ValidationStrategyFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        factory = new ValidationStrategyFactory(validationOrchestrator, responseGenerator);
    }

    @Test
    @DisplayName("Should return WithRetryValidationStrategy when allowRetry is TRUE")
    void testCreateStrategy_AllowRetryTrue() {
        ValidationStrategy strategy = factory.createStrategy(Boolean.TRUE);

        assertNotNull(strategy);
        assertInstanceOf(WithRetryValidationStrategy.class, strategy);
    }

    @Test
    @DisplayName("Should return WithoutRetryValidationStrategy when allowRetry is FALSE")
    void testCreateStrategy_AllowRetryFalse() {
        ValidationStrategy strategy = factory.createStrategy(Boolean.FALSE);

        assertNotNull(strategy);
        assertInstanceOf(WithoutRetryValidationStrategy.class, strategy);
    }

    @Test
    @DisplayName("Should return WithoutRetryValidationStrategy when allowRetry is null")
    void testCreateStrategy_AllowRetryNull() {
        ValidationStrategy strategy = factory.createStrategy(null);

        assertNotNull(strategy);
        assertInstanceOf(WithoutRetryValidationStrategy.class, strategy);
    }

    @Test
    @DisplayName("Should create a new strategy instance on every invocation")
    void testCreateStrategy_ReturnsNewInstanceEachTime() {
        ValidationStrategy first = factory.createStrategy(Boolean.TRUE);
        ValidationStrategy second = factory.createStrategy(Boolean.TRUE);

        assertNotSame(first, second, "Factory should not cache/reuse strategy instances");
    }
}
