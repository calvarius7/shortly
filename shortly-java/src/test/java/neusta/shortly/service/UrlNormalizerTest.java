package neusta.shortly.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class UrlNormalizerTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("normalize should return input as is for null, empty or blank strings")
    void normalize_NullOrEmptyOrBlank(final String input) {
        assertThat(UrlNormalizer.normalize(input)).isEqualTo(input);
    }

    @ParameterizedTest
    @CsvSource({
            "google.com, https://google.com",
            "www.google.com, https://www.google.com",
            " foo.bar , https://foo.bar",
            "sub.domain.tld/path, https://sub.domain.tld/path"
    })
    @DisplayName("normalize should add https:// prefix if scheme is missing")
    void normalize_AddsHttpsIfMissing(final String input, final String expected) {
        assertThat(UrlNormalizer.normalize(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://google.com",
            "https://google.com",
            "ftp://files.server.com",
            "custom-scheme://test",
            "git+ssh://github.com"
    })
    @DisplayName("normalize should keep existing scheme")
    void normalize_KeepsExistingScheme(final String input) {
        assertThat(UrlNormalizer.normalize(input)).isEqualTo(input);
    }

    @Test
    @DisplayName("normalize should trim whitespace even if scheme is present")
    void normalize_TrimsWhitespaceWithScheme() {
        assertThat(UrlNormalizer.normalize("  https://google.com  ")).isEqualTo("https://google.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"google.com", "www.test.de", "mailto:info@test.de"})
    @DisplayName("hasScheme should return false for URLs without :// scheme")
    void hasScheme_False(final String input) {
        assertThat(UrlNormalizer.hasScheme(input)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://google.com",
            "https://www.test.de",
            "ftp://files",
            "a://b",
            "git+ssh://host"
    })
    @DisplayName("hasScheme should return true for valid schemes with ://")
    void hasScheme_True(final String input) {
        assertThat(UrlNormalizer.hasScheme(input)).isTrue();
    }
}
