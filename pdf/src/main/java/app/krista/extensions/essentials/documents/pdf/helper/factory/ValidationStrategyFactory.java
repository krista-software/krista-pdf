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
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

/**
 * Factory for creating validation strategies.
 *
 * <p>This class implements the Factory Pattern to create appropriate validation
 * strategies based on the retry flag.</p>
 *
 * <p><b>Design Patterns Applied:</b></p>
 * <ul>
 *   <li><b>Factory Pattern:</b> Encapsulates object creation logic</li>
 *   <li><b>Strategy Pattern:</b> Creates different strategy implementations</li>
 * </ul>
 *
 * <p><b>SOLID Principles Applied:</b></p>
 * <ul>
 *   <li><b>Single Responsibility:</b> Only responsible for creating validation strategies</li>
 *   <li><b>Open/Closed:</b> Open for extension (new strategies), closed for modification</li>
 *   <li><b>Dependency Inversion:</b> Returns interface type, not concrete implementations</li>
 * </ul>
 */
@Service
public class ValidationStrategyFactory {

    private final ValidationOrchestrator validationOrchestrator;
    private final ExtensionResponseGenerator responseGenerator;

    @Inject
    public ValidationStrategyFactory(ValidationOrchestrator validationOrchestrator,
                                     ExtensionResponseGenerator responseGenerator) {
        this.validationOrchestrator = validationOrchestrator;
        this.responseGenerator = responseGenerator;
    }

    /**
     * Creates appropriate validation strategy based on retry flag.
     *
     * @param allowRetry Whether retry is allowed
     * @return ValidationStrategy instance
     */
    public ValidationStrategy createStrategy(Boolean allowRetry) {
        if (Boolean.TRUE.equals(allowRetry)) {
            return new WithRetryValidationStrategy(validationOrchestrator, responseGenerator);
        } else {
            return new WithoutRetryValidationStrategy();
        }
    }
}

