package neusta.shortly.web.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FlexibleUrl.UrlValidator")
class FlexibleUrlValidatorTest {

    private final FlexibleUrl.UrlValidator validator = new FlexibleUrl.UrlValidator();


    @Test
    @DisplayName("true for null")
    void nullIsValid() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @ParameterizedTest(name = "true for \"{0}\"")
    @ValueSource(strings = {"", " ", "  "})
    @DisplayName("true for blank strings")
    void blankIsValid(final String blank) {
        assertThat(validator.isValid(blank, null)).isTrue();
    }

    @ParameterizedTest(name = "true for {0}")
    @ValueSource(strings = {
            "https://www.google.com",
            "http://example.org",
            "ftp://files.test.com"
    })
    @DisplayName("true for already valid URLs")
    void validUrls(final String url) {
        assertThat(validator.isValid(url, null)).isTrue();
    }

    @ParameterizedTest(name = "true for {0}")
    @ValueSource(strings = {
            "google.com",
            "www.neusta.de",
            "sub.domain.tld/path?query=1"
    })
    @DisplayName("true for URLs without scheme (will be normalized)")
    void normalizableUrls(final String url) {
        assertThat(validator.isValid(url, null)).isTrue();
    }

    @ParameterizedTest(name = "false for {0}")
    @ValueSource(strings = {
            "http://",
            "just text",
            "htt p://wrong.com"
    })
    @DisplayName("false for invalid URLs")
    void invalidUrls(final String url) {
        assertThat(validator.isValid(url, null)).isFalse();
    }

}
