package neusta.shortly.web;

import neusta.shortly.TestcontainersConfiguration;
import neusta.shortly.persistence.ShortLinkRepository;
import neusta.shortly.web.dto.ShortLinkDto;
import neusta.shortly.web.dto.StatsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.net.URI;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ControllerIT {

    @LocalServerPort
    int port;
    private WebTestClient webTestClient;
    @Autowired
    private ShortLinkRepository repository;

    @BeforeEach
    void setupClient() {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void resolveShortCode() {
        // arrange: create a short link via API
        final String originalUrl = "https://example.org/target";
        final String shortCode = createShortLink(originalUrl);

        // act + assert: resolving returns 302 with Location header
        webTestClient.get()
                .uri("/{shortCode}", shortCode)
                .exchange()
                .expectStatus().isFound()
                .expectHeader().valueEquals(LOCATION, URI.create(originalUrl).toString());

        // and clicks are incremented in Redis (service increments on read)
        final var saved = repository.findById(shortCode).orElseThrow();
        assertThat(saved.getClicks()).isEqualTo(1);
    }

    @Test
    void getStats() {
        // arrange: create a short link via API
        final String originalUrl = "https://example.org/stats";
        final String shortCode = createShortLink(originalUrl);

        // act + assert: stats endpoint returns clicks, note: service increments on read
        webTestClient.get()
                .uri("/api/stats/{shortCode}", shortCode)
                .exchange()
                .expectStatus().isOk()
                .expectBody(StatsDto.class)
                .value(dto -> {
                    assertThat(dto).isNotNull();
                    assertThat(dto.shortCode()).isEqualTo(shortCode);
                    assertThat(dto.clicks()).isEqualTo(1); // first read increments to 1
                });

        // verify Redis state matches
        final var saved = repository.findById(shortCode).orElseThrow();
        assertThat(saved.getClicks()).isEqualTo(1);
    }

    @Test
    void create() {
        final String originalUrl = "https://example.org/create";
        final Instant expiresAt = Instant.now().plusSeconds(3600);

        final EntityExchangeResult<String> result = webTestClient.post()
                .uri("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ShortLinkDto(originalUrl, expiresAt))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult();

        final String shortCode = result.getResponseBody();
        assertThat(shortCode).isNotNull().isNotBlank();

        final var saved = repository.findById(shortCode).orElseThrow();
        assertThat(saved.getOriginalUrl()).isEqualTo(originalUrl);
        // ttl is managed by Redis; we only assert it was set for a future expiration
        assertThat(saved.getTtl()).isNotNull();
    }

    @Test
    void delete() {
        // arrange: create a short link via API
        final String originalUrl = "https://example.org/to-delete";
        final String shortCode = createShortLink(originalUrl);

        // act: delete
        webTestClient.delete()
                .uri("/api/{shortCode}", shortCode)
                .exchange()
                .expectStatus().isNoContent();

        // assert: entity removed from Redis
        assertThat(repository.existsById(shortCode)).isFalse();
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
        final String shortCode = result.getResponseBody();
        assertThat(shortCode).isNotNull().isNotBlank();
        return shortCode;
    }
}
