package neusta.shortly.web.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShortLinkDto Bean Validation")
class ShortLinkDtoValidationTest {

    static Validator validator;
    static Locale defaultLocale;

    @BeforeAll
    static void setup() {
        // Sicherstellen, dass deutsche Messages aus ValidationMessages.properties verwendet werden
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMAN);
        final var factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        Locale.setDefault(defaultLocale);
    }

    @Nested
    @DisplayName("valid")
    class ValidCases {
        @Test
        @DisplayName("gültige URL + expiresAt in der Zukunft")
        void valid() {
            final var dto = new ShortLinkDto("https://example.org/ok", Instant.now().plusSeconds(60));
            final var violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("invalid")
    class InvalidCases {
        @Test
        @DisplayName("leere URL")
        void blankUrl() {
            final var dto = new ShortLinkDto("", Instant.now().plusSeconds(60));
            final var violations = validator.validate(dto);
            assertThat(violations).anySatisfy(v -> assertThat(v.getMessage()).contains("URL darf nicht leer sein"));
        }

        @Test
        @DisplayName("ungültige URL")
        void invalidUrl() {
            final var dto = new ShortLinkDto("not-a-url", Instant.now().plusSeconds(60));
            final var violations = validator.validate(dto);
            assertThat(violations).anySatisfy(v -> assertThat(v.getMessage()).contains("Muss eine gültige URL sein"));
        }

        @Test
        @DisplayName("expiresAt in der Vergangenheit")
        void pastExpiry() {
            final var dto = new ShortLinkDto("https://example.org/ok", Instant.now().minusSeconds(5));
            final var violations = validator.validate(dto);
            assertThat(violations).anySatisfy(v -> assertThat(v.getMessage()).contains("Das Verfallsdatum muss in der Zukunft liegen"));
        }
    }
}
