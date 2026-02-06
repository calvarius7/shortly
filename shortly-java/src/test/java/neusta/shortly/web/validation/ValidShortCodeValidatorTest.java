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
        @DisplayName("true for exactly " + ShortCodeGenerator.LENGTH + " chars")
        void validLength() {
            assertThat(validator.isValid("A".repeat(ShortCodeGenerator.LENGTH), null)).isTrue();
        }

        @Test
        void nullIsInvalid() {
            assertThat(validator.isValid(null, null)).isFalse();
        }

        @Test
        @DisplayName("returns false for invalid lengths")
        void wrongLengths() {
            assertThat(validator.isValid("ABCDE", null)).isFalse();
            assertThat(validator.isValid("ABCDEFG", null)).isFalse();
        }
    }
}
