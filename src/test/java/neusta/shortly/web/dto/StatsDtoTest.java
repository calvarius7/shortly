package neusta.shortly.web.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StatsDto Record/Builder")
class StatsDtoTest {

    @Test
    @DisplayName("Builder setzt Felder korrekt und Record liefert Werte")
    void builderAndAccessors() {
        StatsDto dto = StatsDto.of().shortCode("ABC123").clicks(42).build();
        assertThat(dto.shortCode()).isEqualTo("ABC123");
        assertThat(dto.clicks()).isEqualTo(42);
        assertThat(dto.toString()).contains("ABC123").contains("42");
    }
}
