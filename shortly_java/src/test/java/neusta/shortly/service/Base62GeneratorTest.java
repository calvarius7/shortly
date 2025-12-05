package neusta.shortly.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Base62Generator")
class Base62GeneratorTest {

    private static final Pattern BASE62_PATTERN = Pattern.compile("[0-9a-zA-Z]{" + ShortCodeGenerator.LENGTH + "}");
    private final Base62Generator generator = new Base62Generator();

    @Test
    @DisplayName("liefert einen Code mit fixer Länge")
    void lengthIsFixed() {
        final String code = generator.generate();
        assertThat(code).hasSize(ShortCodeGenerator.LENGTH);
    }

    @Test
    @DisplayName("enthält nur Base62-Zeichen")
    void containsOnlyBase62Chars() {
        final String code = generator.generate();
        assertThat(BASE62_PATTERN.matcher(code).matches()).isTrue();
    }

    @Test
    @DisplayName("ist hinreichend zufällig (mehrere unterschiedliche Codes)")
    void seemsRandomEnough() {
        final Set<String> set = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            set.add(generator.generate());
        }
        assertThat(set.size()).isGreaterThan(1);

    }
}
