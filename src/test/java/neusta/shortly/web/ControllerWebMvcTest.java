package neusta.shortly.web;

import neusta.shortly.model.ShortLink;
import neusta.shortly.service.ShortLinkService;
import neusta.shortly.web.dto.ShortLinkDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controller (Unit)")
class ControllerWebMvcTest {

    @Mock
    ShortLinkService shortLinkService;

    @InjectMocks
    Controller controller;

    @Nested
    @DisplayName("resolveShortCode")
    class ResolveShortCode {
        @Test
        @DisplayName("liefert 302 mit Location, wenn gefunden")
        void found() {
            ShortLink entity = ShortLink.of().shortCode("ABC123").originalUrl("https://example.org").clicks(0).ttl(null).build();
            when(shortLinkService.findById("ABC123")).thenReturn(Optional.of(entity));

            ResponseEntity<Void> response = controller.resolveShortCode("ABC123");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
            assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("https://example.org"));
        }

        @Test
        @DisplayName("liefert 404, wenn nicht gefunden")
        void notFound() {
            when(shortLinkService.findById("ZZZZZZ")).thenReturn(Optional.empty());
            ResponseEntity<Void> response = controller.resolveShortCode("ZZZZZZ");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getStats")
    class GetStats {
        @Test
        @DisplayName("liefert 200 und Stats")
        void ok() {
            ShortLink entity = ShortLink.of().shortCode("ABC123").originalUrl("https://e").clicks(5).ttl(null).build();
            when(shortLinkService.findById("ABC123")).thenReturn(Optional.of(entity));

            ResponseEntity<?> response = controller.getStats("ABC123");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull().hasToString("StatsDto[shortCode=ABC123, clicks=5]");
        }

        @Test
        @DisplayName("liefert 404, wenn nicht gefunden")
        void notFound() {
            when(shortLinkService.findById("ABC123")).thenReturn(Optional.empty());
            ResponseEntity<?> response = controller.getStats("ABC123");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("create")
    class CreateShortLink {
        @Test
        @DisplayName("liefert ShortCode bei gültiger Eingabe")
        void created() {
            when(shortLinkService.create(org.mockito.ArgumentMatchers.eq("https://example.org/x"), any())).thenReturn(
                    ShortLink.of().shortCode("NEW123").originalUrl("https://example.org/x").clicks(0).ttl(100L).build()
            );
            ShortLinkDto dto = new ShortLinkDto("https://example.org/x", Instant.now().plusSeconds(60));
            String result = controller.create(dto);
            assertThat(result).isEqualTo("NEW123");
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteShortLink {
        @Test
        @DisplayName("delegiert an Service")
        void deletes() {
            doNothing().when(shortLinkService).deleteById("ABC123");
            controller.delete("ABC123");
        }
    }
}
