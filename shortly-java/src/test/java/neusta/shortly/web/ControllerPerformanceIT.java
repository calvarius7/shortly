package neusta.shortly.web;

import neusta.shortly.TestcontainersConfiguration;
import neusta.shortly.web.dto.ShortLinkDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ControllerPerformanceIT {

    @LocalServerPort
    int port;
    private WebTestClient webTestClient;

    @BeforeEach
    void setupClient() {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Test
    void redirectPerformance_shouldCompleteUnder100ms() {
        // Given: Create a short link
        final String shortCode = createShortLink("https://example.org/performance");

        // When: Measure redirect response time
        final long startTime = System.nanoTime();
        webTestClient.get()
                .uri("/{shortCode}", shortCode)
                .exchange()
                .expectStatus().isFound();
        final long endTime = System.nanoTime();

        // Then: Response time should be < 100ms
        final long durationMs = (endTime - startTime) / 1_000_000;
        assertThat(durationMs)
                .as("Redirect should complete in under 100ms")
                .isLessThan(100);
    }

    @Test
    void createShortLinkPerformance_shouldCompleteUnder200ms() {
        // Given: Prepare request
        final ShortLinkDto dto = new ShortLinkDto(
                "https://example.org/create-performance",
                Instant.now().plusSeconds(600));

        // When: Measure creation time
        final long startTime = System.nanoTime();
        webTestClient.post()
                .uri("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isCreated();
        final long endTime = System.nanoTime();

        // Then: Response time should be < 200ms
        final long durationMs = (endTime - startTime) / 1_000_000;
        assertThat(durationMs)
                .as("ShortLink creation should complete in under 200ms")
                .isLessThan(200);
    }

    private String createShortLink(final String originalUrl) {
        final Instant expiresAt = Instant.now().plusSeconds(600);
        final EntityExchangeResult<String> result = webTestClient.post()
                .uri("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ShortLinkDto(originalUrl, expiresAt))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult();
        return result.getResponseBody();
    }
}
