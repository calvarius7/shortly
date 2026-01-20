package neusta.shortly.service;

import neusta.shortly.model.ShortLink;
import neusta.shortly.persistence.ShortLinkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortLinkService")
class ShortLinkServiceTest {

    @Mock
    ShortLinkRepository repository;

    @Mock
    ShortCodeGenerator generator;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    StringRedisTemplate redisTemplate;

    @InjectMocks
    ShortLinkService service;

    @Test
    @DisplayName("deleteById() delegiert an Repository")
    void deleteDelegates() {
        service.deleteById("DEL123");
        verify(repository).deleteById("DEL123");
    }

    @Nested
    @DisplayName("create()")
    class Create {
        @Test
        @DisplayName("erzeugt eindeutigen Code und speichert mit TTL")
        void createsUniqueAndSavesWithTtl() {
            when(generator.generate()).thenReturn("dupdup", "unique1");
            when(repository.existsById("dupdup")).thenReturn(true);
            when(repository.existsById("unique1")).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            final Instant expires = Instant.now().plusSeconds(120);
            final ShortLink saved = service.create("https://example.org/x", expires);

            assertThat(saved.getShortCode()).isEqualTo("unique1");
            assertThat(saved.getOriginalUrl()).isEqualTo("https://example.org/x");
            assertThat(saved.getTtl()).isNotNull().isPositive();

            verify(repository).save(any(ShortLink.class));
        }

        @Test
        @DisplayName("setzt TTL auf null wenn expiresAt null ist")
        void ttlNullWhenNoExpiry() {
            when(generator.generate()).thenReturn("ABC123");
            when(repository.existsById("ABC123")).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            final ShortLink saved = service.create("https://example.org/no-expire", null);
            assertThat(saved.getTtl()).isNull();
        }

        @Test
        @DisplayName("setzt TTL auf null wenn expiresAt in der Vergangenheit liegt")
        void ttlNullWhenPast() {
            when(generator.generate()).thenReturn("PAST12");
            when(repository.existsById("PAST12")).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            final ShortLink saved = service.create("https://example.org/past", Instant.now().minusSeconds(5));
            assertThat(saved.getTtl()).isNull();
        }
    }

    @Nested
    @DisplayName("resolveAndTrack()")
    class FindById {
        @Test
        @DisplayName("erhöht Klickzähler und speichert bei Treffer")
        void incrementsClicksOnHit() {
            final var key = "ABC123";
            final ShortLink entity = ShortLink.of()
                    .shortCode(key)
                    .originalUrl("https://e.org")
                    .clicks(2)
                    .ttl(null)
                    .build();
            when(repository.findById(key)).thenReturn(Optional.of(entity));
            when(redisTemplate.opsForHash().increment(ShortLink.getRedisKey(key), "clicks", 1)).thenReturn(3L);

            final Optional<ShortLink> result = service.resolveAndTrack(key);
            assertThat(result).isPresent();

            assertThat(result.get().getClicks()).isEqualTo(3);
        }

        @Test
        @DisplayName("liefert empty ohne Speicherung wenn nicht gefunden")
        void emptyWhenNotFound() {
            when(repository.findById("ZZZZZZ")).thenReturn(Optional.empty());

            final Optional<ShortLink> result = service.resolveAndTrack("ZZZZZZ");
            assertThat(result).isEmpty();
            verify(repository, never()).save(any());
        }
    }
}
