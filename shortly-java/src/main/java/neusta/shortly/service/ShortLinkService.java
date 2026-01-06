package neusta.shortly.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import neusta.shortly.model.ShortLink;
import neusta.shortly.persistence.ShortLinkRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShortLinkService {

    private final ShortLinkRepository repository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final StringRedisTemplate redisTemplate;

    public ShortLink create(final String originalUrl, @Nullable final Instant expiresAt) {
        return repository.save(ShortLink.of()
                .shortCode(generateUniqueShortCode())
                .originalUrl(originalUrl)
                .ttl(calculateTtl(expiresAt))
                .build());
    }

    private String generateUniqueShortCode() {
        String candidate;
        do {
            candidate = shortCodeGenerator.generate();
        } while (repository.existsById(candidate));
        return candidate;
    }

    public Optional<ShortLink> findById(final String id) {
        return repository.findById(id)
                .map(shortLink -> {
                    // Atomic increment using Redis HINCRBY
                    final Long clicks = redisTemplate.opsForHash()
                            .increment(ShortLink.getRedisKey(id), "clicks", 1);
                    shortLink.setClicks(clicks.intValue());
                    return shortLink;
                });
    }

    public void deleteById(final String id) {
        repository.deleteById(id);
    }

    private Long calculateTtl(final Instant expiresAt) {
        if (expiresAt == null) {
            return null;
        }
        final var seconds = Duration.between(Instant.now(), expiresAt).getSeconds();
        if (seconds <= 0) {
            return null; // or throw exception?
        }
        return seconds;
    }
}
