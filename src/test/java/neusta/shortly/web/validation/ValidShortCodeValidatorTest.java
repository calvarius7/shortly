package neusta.shortly.web.validation;

import neusta.shortly.service.ShortCodeGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidShortCode.ShortCodeValidator")
class ValidShortCodeValidatorTest {

    private final ValidShortCode.ShortCodeValidator validator = new ValidShortCode.ShortCodeValidator();

    @Nested
    @DisplayName("isValid()")
    class IsValid {
        @Test
        @DisplayName("gibt true für exakt " + ShortCodeGenerator.LENGTH + " Zeichen zurück")
        void validLength() {
            assertThat(validator.isValid("A".repeat(ShortCodeGenerator.LENGTH), null)).isTrue();
        }

        @Test
        @DisplayName("gibt false für null zurück")
        void nullIsInvalid() {
            assertThat(validator.isValid(null, null)).isFalse();
        }

        @Test
        @DisplayName("gibt false für andere Längen zurück")
        void wrongLengths() {
            assertThat(validator.isValid("ABCDE", null)).isFalse();
            assertThat(validator.isValid("ABCDEFG", null)).isFalse();
        }
    }
}
