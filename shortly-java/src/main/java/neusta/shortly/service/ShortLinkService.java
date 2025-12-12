package neusta.shortly.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import neusta.shortly.model.ShortLink;
import neusta.shortly.persistence.ShortLinkRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ShortLinkService {

    private final ShortLinkRepository repository;
    private final ShortCodeGenerator shortCodeGenerator;

    public ShortLink create(final String originalUrl, @Nullable final Instant expiresAt) {
        final var id = Stream.generate(shortCodeGenerator::generate)
                .filter(generated -> !repository.existsById(generated))
                .findFirst()
                .orElseThrow();

        return repository.save(ShortLink.of()
                .shortCode(id)
                .originalUrl(originalUrl)
                .ttl(calculateTtl(expiresAt))
                .build());
    }

    public Optional<ShortLink> findById(final String id) {
        return repository.findById(id)
                .map(shortLink -> {
                    shortLink.setClicks(shortLink.getClicks() + 1);
                    return repository.save(shortLink);
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
